package com.github.cabutchei.rsp.eclipse.wst.wtp;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.j2ee.internal.componentcore.JavaEEModuleHandler;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.IModuleHandler;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.workspace.DeployableArtifact;
import com.github.cabutchei.rsp.server.spi.workspace.DeploymentAssemblyEntry;
import com.github.cabutchei.rsp.server.spi.workspace.IWTPService;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceProject;

/**
 * Temporary home for WTP/WST-specific workspace operations.
 */
public class WTPService implements IWTPService {
	private static final String BUNDLE_ID = "com.github.cabutchei.rsp.server.wst";
	private static final String KIND_FOLDER = "folder";
	private static final String KIND_PROJECT = "project";
	private static final String KIND_ARCHIVE = "archive";
	private static final long WORKSPACE_DEPLOYABLES_TTL_NANOS = TimeUnit.SECONDS.toNanos(15);
	private static final long REBUILD_DEBOUNCE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);

	private final Set<Path> workspaceRoots = new LinkedHashSet<>();
	private final Object workspaceDeployablesCacheLock = new Object();
	private volatile long workspaceDeployablesGeneration = 1L;
	private volatile long workspaceDeployablesRebuildNotBeforeNanos;
	private volatile CacheValue<DeployableArtifact> workspaceDeployablesCache;
	private final IResourceChangeListener workspaceChangeListener = this::onWorkspaceChanged;

	public WTPService() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(workspaceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void setWorkspaceRoots(Collection<Path> workspaceRoots) {
		Collection<Path> normalized = normalizeRoots(workspaceRoots);
		synchronized (this.workspaceRoots) {
			this.workspaceRoots.clear();
			this.workspaceRoots.addAll(normalized);
		}
		invalidateWorkspaceDeployablesCache();
	}

	@Override
	public List<DeployableArtifact> listDeployableResources(ServerHandle server) {
		List<DeployableArtifact> deployables;
		if (server == null) {
			deployables = getWorkspaceDeployablesCached();
		} else {
			deployables = listServerDeployables(server);
		}
		if (deployables == null || deployables.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		if (roots.isEmpty()) {
			return deployables;
		}
		List<DeployableArtifact> filtered = new ArrayList<>();
		for (DeployableArtifact deployable : deployables) {
			if (deployable == null) {
				continue;
			}
			Path deployPath = deployable.getDeployPath();
			if (deployPath == null) {
				continue;
			}
			Path normalized = deployPath.toAbsolutePath().normalize();
			if (isContainedInAny(normalized, roots)) {
				filtered.add(deployable);
			}
		}
		return filtered;
	}

	@Override
	public List<WorkspaceProject> listDeploymentAssemblyProjects(Path projectPath, String projectName) {
		List<IProject> projects = listAvailableDeploymentAssemblyProjects(projectPath, projectName);
		if (projects == null || projects.isEmpty()) {
			return Collections.emptyList();
		}
		List<WorkspaceProject> mapped = new ArrayList<>();
		for (IProject project : projects) {
			if (project == null) {
				continue;
			}
			IPath location = project.getLocation();
			Path path = location == null ? null : location.toFile().toPath();
			mapped.add(new WorkspaceProject(project.getName(), path, project.isOpen()));
		}
		return mapped;
	}

	@Override
	public List<DeploymentAssemblyEntry> getDeploymentAssembly(Path projectPath, String projectName) {
		IProject project = resolveProject(projectPath, projectName);
		if (project == null || !project.exists()) {
			return null;
		}
		try {
			if (!project.isOpen()) {
				project.open(new NullProgressMonitor());
			}
		} catch (CoreException ce) {
			return null;
		}
		List<DeploymentAssemblyEntry> entries = readDeploymentAssembly(project);
		return entries == null ? null : Collections.unmodifiableList(entries);
	}

	@Override
	public IStatus addDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry) {
		IProject project = resolveProject(projectPath, projectName);
		if (project == null || !project.exists()) {
			return errorStatus("Project not found", null);
		}
		try {
			if (!project.isOpen()) {
				project.open(new NullProgressMonitor());
			}
		} catch (CoreException ce) {
			return errorStatus("Failed to open project " + project.getName(), ce);
		}
		IStatus status = addDeploymentAssemblyEntryInternal(project, entry);
		if (status != null && status.isOK()) {
			invalidateWorkspaceDeployablesCache();
		}
		return status;
	}

	@Override
	public IStatus removeDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry) {
		IProject project = resolveProject(projectPath, projectName);
		if (project == null || !project.exists()) {
			return errorStatus("Project not found", null);
		}
		try {
			if (!project.isOpen()) {
				project.open(new NullProgressMonitor());
			}
		} catch (CoreException ce) {
			return errorStatus("Failed to open project " + project.getName(), ce);
		}
		IStatus status = removeDeploymentAssemblyEntryInternal(project, entry);
		if (status != null && status.isOK()) {
			invalidateWorkspaceDeployablesCache();
		}
		return status;
	}

	@Override
	public IStatus ensureFacets(String projectName, List<String> facetIds) {
		return errorStatus("Facet operations are not implemented", null);
	}

	@Override
	public IStatus updateFacets(String projectName, List<String> add, List<String> remove) {
		return errorStatus("Facet operations are not implemented", null);
	}

	private List<DeployableArtifact> listServerDeployables(ServerHandle server) {
		if (server == null) {
			return Collections.emptyList();
		}
		org.eclipse.wst.server.core.IServer wstServer = ServerCore.findServer(server.getId());
		if (wstServer == null || wstServer.getServerType() == null || wstServer.getServerType().getRuntimeType() == null) {
			return Collections.emptyList();
		}
		IModule[] deployedModules = wstServer.getModules();
		Set<IModule> deployed = deployedModules == null
				? Collections.emptySet()
				: new HashSet<>(Arrays.asList(deployedModules));
		IModuleType[] moduleTypes = wstServer.getServerType().getRuntimeType().getModuleTypes();
		IModule[] modules = moduleTypes == null ? new IModule[0] : ServerUtil.getModules(moduleTypes);
		List<DeployableArtifact> results = new ArrayList<>();
			Set<String> seenModules = new HashSet<>();
		if (modules == null) {
			return results;
		}
		for (IModule module : modules) {
			if (module == null || deployed.contains(module)) {
				continue;
			}
			IModule[] parents;
			try {
				parents = wstServer.getRootModules(module, null);
			} catch (CoreException ce) {
				continue;
			}
			if (parents == null || parents.length == 0) {
				continue;
			}
			boolean isRoot = false;
			for (IModule parent : parents) {
				if (module.equals(parent)) {
					isRoot = true;
					break;
				}
			}
			if (!isRoot) {
				continue;
			}
			org.eclipse.core.runtime.IStatus status = wstServer.canModifyModules(new IModule[] { module }, null, null);
			if (status != null && !status.isOK()) {
				continue;
			}
			String moduleId = module.getId();
			if (moduleId != null && !seenModules.add(moduleId)) {
				continue;
			}
			IProject project = module.getProject();
			if (project == null) {
				continue;
			}
			IPath location = project.getLocation();
			// TODO: should no longer point to the project' path, but to the module's
			Path deployPath = location == null ? null : location.toFile().toPath();
			String typeId = module.getModuleType() == null ? null : module.getModuleType().getId();
				results.add(new DeployableArtifact(moduleId, project.getName(), ServerUtil.getModuleDisplayName(module), deployPath, typeId));
		}
		return results;
	}

	private List<IProject> listAvailableDeploymentAssemblyProjects(Path projectPath, String projectName) {
		IProject project = resolveProject(projectPath, projectName);
		if (project == null || !project.exists()) {
			return Collections.emptyList();
		}
		try {
			if (!project.isOpen()) {
				project.open(new NullProgressMonitor());
			}
		} catch (CoreException ce) {
			return Collections.emptyList();
		}
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return Collections.emptyList();
		}
		IVirtualReference[] refs = rootComponent.getReferences();
		ArrayList<IVirtualReference> currentRefs = refs == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(refs));
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<IProject> availableList = getAvailableProjects(projects, currentRefs);
		IModuleHandler handler = resolveModuleHandler(rootComponent);
		List<IProject> filtered = handler.getFilteredProjectListForAdd(rootComponent, availableList);
		return filtered == null ? Collections.emptyList() : filtered;
	}

	private List<DeploymentAssemblyEntry> readDeploymentAssembly(IProject project) {
		if (project == null || !project.exists()) {
			return null;
		}
		List<DeploymentAssemblyEntry> entries = new ArrayList<>();
		addComponentResourceMappings(project, entries);
		addComponentReferences(project, entries);
		return entries;
	}

	private void addComponentResourceMappings(IProject project, List<DeploymentAssemblyEntry> entries) {
		StructureEdit structureEdit = null;
		try {
			structureEdit = StructureEdit.getStructureEditForRead(project);
			WorkbenchComponent component = structureEdit.getComponent();
			if (component == null) {
				return;
			}
			Object[] resources = component.getResources().toArray();
			for (Object resourceObj : resources) {
				if (!(resourceObj instanceof ComponentResource)) {
					continue;
				}
				ComponentResource resource = (ComponentResource) resourceObj;
				IPath sourcePath = resource.getSourcePath();
				IPath runtimePath = resource.getRuntimePath();
				if (sourcePath == null || runtimePath == null) {
					continue;
				}
				entries.add(new DeploymentAssemblyEntry(sourcePath.toString(), normalizeRuntimePath(runtimePath), KIND_FOLDER,
						KIND_FOLDER));
			}
		} catch (Exception e) {
			return;
		} catch (Throwable e) {
			return;
		} finally {
			if (structureEdit != null) {
				structureEdit.dispose();
			}
		}
	}

	private void addComponentReferences(IProject project, List<DeploymentAssemblyEntry> entries) {
		IVirtualComponent component = ComponentCore.createComponent(project);
		if (component == null) {
			return;
		}
		HashMap<String, Object> options = new HashMap<>();
		options.put(IVirtualComponent.REQUESTED_REFERENCE_TYPE, IVirtualComponent.DISPLAYABLE_REFERENCES_ALL);
		IVirtualReference[] refs = component.getReferences(options);
		if (refs == null) {
			return;
		}
		for (IVirtualReference ref : refs) {
			if (ref == null) {
				continue;
			}
			String sourceText = resolveReferenceSource(ref.getReferencedComponent());
			String runtimeText = normalizeRuntimePath(new org.eclipse.core.runtime.Path(getSafeRuntimePath(ref)));
			entries.add(new DeploymentAssemblyEntry(sourceText, runtimeText, resolveReferenceSourceKind(ref.getReferencedComponent()),
					KIND_ARCHIVE));
		}
	}

	private String resolveReferenceSource(IVirtualComponent component) {
		if (component == null) {
			return "";
		}
		if (component.isBinary()) {
			IPath componentPath = component.getAdapter(IPath.class);
			return componentPath == null ? component.getName() : componentPath.toString();
		}
		IProject project = component.getProject();
		return project == null ? component.getName() : project.getName();
	}

	private String resolveReferenceSourceKind(IVirtualComponent component) {
		if (component != null && component.isBinary()) {
			return KIND_ARCHIVE;
		}
		return KIND_PROJECT;
	}

	private String getSafeRuntimePath(IVirtualReference ref) {
		String archiveName = ref.getDependencyType() == IVirtualReference.DEPENDENCY_TYPE_CONSUMES ? null : ref.getArchiveName();
		String value;
		if (archiveName != null) {
			IPath runtimePath = new org.eclipse.core.runtime.Path(archiveName);
			if (runtimePath.segmentCount() > 1) {
				value = archiveName;
			} else {
				value = ref.getRuntimePath().append(archiveName).toString();
			}
		} else {
			value = ref.getRuntimePath().toString();
		}
		return value == null ? "/" : value;
	}

	private String normalizeRuntimePath(IPath runtimePath) {
		if (runtimePath.isRoot()) {
			return runtimePath.toString();
		}
		return runtimePath.makeRelative().toString();
	}

	private IStatus addDeploymentAssemblyEntryInternal(IProject project, DeploymentAssemblyEntry entry) {
		if (project == null || !project.exists()) {
			return errorStatus("Project not found", null);
		}
		if (entry == null) {
			return errorStatus("Entry is required", null);
		}
		if (isResourceMapping(entry)) {
			return addResourceMapping(project, entry);
		}
		return addReference(project, entry);
	}

	private IStatus removeDeploymentAssemblyEntryInternal(IProject project, DeploymentAssemblyEntry entry) {
		if (project == null || !project.exists()) {
			return errorStatus("Project not found", null);
		}
		if (entry == null) {
			return errorStatus("Entry is required", null);
		}
		if (isResourceMapping(entry)) {
			return removeResourceMapping(project, entry);
		}
		return removeReference(project, entry);
	}

	private boolean isResourceMapping(DeploymentAssemblyEntry entry) {
		return KIND_FOLDER.equals(entry.getSourceKind()) && KIND_FOLDER.equals(entry.getDeployKind());
	}

	private IStatus addResourceMapping(IProject project, DeploymentAssemblyEntry entry) {
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return errorStatus("Component not available", null);
		}
		String deployPath = entry.getDeployPath() == null ? "/" : entry.getDeployPath();
		IPath runtimePath = new org.eclipse.core.runtime.Path(deployPath).makeAbsolute();
		IPath sourcePath = new org.eclipse.core.runtime.Path(entry.getSourcePath());
		IVirtualFolder rootFolder = rootComponent.getRootFolder();
		try {
			rootFolder.getFolder(runtimePath).createLink(sourcePath, 0, null);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return errorStatus("Failed to add resource mapping", e);
		}
	}

	private IStatus removeResourceMapping(IProject project, DeploymentAssemblyEntry entry) {
		StructureEdit structureEdit = null;
		try {
			structureEdit = StructureEdit.getStructureEditForWrite(project);
			WorkbenchComponent component = structureEdit.getComponent();
			if (component == null) {
				return errorStatus("Component not available", null);
			}
			String targetRuntimePath = normalizeRuntimePath(
					new org.eclipse.core.runtime.Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath()));
			String targetSource = entry.getSourcePath();
			List<?> resources = component.getResources();
			for (int i = resources.size() - 1; i >= 0; i--) {
				Object obj = resources.get(i);
				if (!(obj instanceof ComponentResource)) {
					continue;
				}
				ComponentResource resource = (ComponentResource) obj;
				IPath sourcePath = resource.getSourcePath();
				IPath runtimePath = resource.getRuntimePath();
				if (sourcePath == null || runtimePath == null) {
					continue;
				}
				String runtimeNormalized = normalizeRuntimePath(runtimePath);
				if (sourcePath.toString().equals(targetSource) && runtimeNormalized.equals(targetRuntimePath)) {
					resources.remove(i);
				}
			}
			return Status.OK_STATUS;
		} finally {
			if (structureEdit != null) {
				structureEdit.saveIfNecessary(new NullProgressMonitor());
				structureEdit.dispose();
			}
		}
	}

	private IStatus addReference(IProject project, DeploymentAssemblyEntry entry) {
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return errorStatus("Component not available", null);
		}
		IVirtualComponent targetComponent = resolveReferencedComponent(project, entry);
		if (targetComponent == null) {
			return errorStatus("Referenced component not found", null);
		}
		IPath runtimePath = new org.eclipse.core.runtime.Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath())
				.makeAbsolute();
		IVirtualReference reference = ComponentCore.createReference(rootComponent, targetComponent, runtimePath);
		if (KIND_ARCHIVE.equals(entry.getSourceKind())) {
			String sourcePath = entry.getSourcePath();
			if (sourcePath != null && !sourcePath.isEmpty()) {
				reference.setArchiveName(new File(sourcePath).getName());
			}
		}
		rootComponent.addReferences(new IVirtualReference[] { reference });
		return Status.OK_STATUS;
	}

	private IStatus removeReference(IProject project, DeploymentAssemblyEntry entry) {
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return errorStatus("Component not available", null);
		}
		String targetSource = entry.getSourcePath();
		String targetDeploy = normalizeRuntimePath(
				new org.eclipse.core.runtime.Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath()));
		IVirtualReference[] refs = rootComponent.getReferences();
		if (refs == null || refs.length == 0) {
			return errorStatus("Reference not found", null);
		}
		ArrayList<IVirtualReference> updated = new ArrayList<>(Arrays.asList(refs));
		boolean removed = false;
		for (IVirtualReference ref : refs) {
			if (ref == null) {
				continue;
			}
			if (!matchesReferenceSource(ref, targetSource, entry.getSourceKind())) {
				continue;
			}
			String runtimeText = normalizeRuntimePath(new org.eclipse.core.runtime.Path(getSafeRuntimePath(ref)));
			if (runtimeText.equals(targetDeploy)) {
				updated.remove(ref);
				removed = true;
			}
		}
		if (!removed) {
			return errorStatus("Reference not found", null);
		}
		rootComponent.setReferences(updated.toArray(new IVirtualReference[0]));
		return Status.OK_STATUS;
	}

	private IVirtualComponent resolveReferencedComponent(IProject project, DeploymentAssemblyEntry entry) {
		if (KIND_PROJECT.equals(entry.getSourceKind())) {
			IProject referenced = getProjectByName(entry.getSourcePath());
			return referenced == null ? null : ComponentCore.createComponent(referenced);
		}
		if (KIND_ARCHIVE.equals(entry.getSourceKind())) {
			String sourcePath = entry.getSourcePath();
			if (sourcePath == null || sourcePath.isEmpty()) {
				return null;
			}
			String componentName = VirtualArchiveComponent.LIBARCHIVETYPE + IPath.SEPARATOR + sourcePath;
			IPath runtimePath = new org.eclipse.core.runtime.Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath())
					.makeAbsolute();
			return ComponentCore.createArchiveComponent(project, componentName, runtimePath);
		}
		return null;
	}

	private boolean matchesReferenceSource(IVirtualReference ref, String targetSource, String sourceKind) {
		IVirtualComponent component = ref.getReferencedComponent();
		if (component == null) {
			return false;
		}
		if (KIND_PROJECT.equals(sourceKind)) {
			return component.getProject() != null && component.getProject().getName().equals(targetSource);
		}
		IPath componentPath = component.getAdapter(IPath.class);
		if (componentPath != null) {
			return componentPath.toString().equals(targetSource);
		}
		return component.getName() != null && component.getName().equals(targetSource);
	}

	private ArrayList<IProject> getAvailableProjects(IProject[] projects, ArrayList<IVirtualReference> currentRefs) {
		if (projects == null || projects.length == 0) {
			return new ArrayList<>();
		}
		if (currentRefs == null || currentRefs.isEmpty()) {
			return new ArrayList<>(Arrays.asList(projects));
		}
		ArrayList<IProject> available = new ArrayList<>();
		for (IProject proj : projects) {
			if (proj == null) {
				continue;
			}
			boolean matches = false;
			for (int j = 0; j < currentRefs.size() && !matches; j++) {
				IVirtualReference ref = currentRefs.get(j);
				if (ref == null) {
					continue;
				}
				IVirtualComponent referenced = ref.getReferencedComponent();
				IProject referencedProject = referenced == null ? null : referenced.getProject();
				if (proj.equals(referencedProject) || available.contains(proj)) {
					matches = true;
				}
			}
			if (!matches) {
				available.add(proj);
			}
		}
		return available;
	}

	private IModuleHandler resolveModuleHandler(IVirtualComponent component) {
		if (component == null) {
			return new JavaEEModuleHandler();
		}
		IModuleHandler handler = component.getAdapter(IModuleHandler.class);
		return handler == null ? new JavaEEModuleHandler() : handler;
	}

	private List<DeployableArtifact> getWorkspaceDeployablesCached() {
		CacheValue<DeployableArtifact> cache = workspaceDeployablesCache;
		long now = System.nanoTime();
		long generation = workspaceDeployablesGeneration;
		if (isCacheFresh(cache, generation, now)) {
			return cache.value;
		}
		synchronized (workspaceDeployablesCacheLock) {
			cache = workspaceDeployablesCache;
			now = System.nanoTime();
			generation = workspaceDeployablesGeneration;
			if (isCacheFresh(cache, generation, now)) {
				return cache.value;
			}
			if (cache != null && now < workspaceDeployablesRebuildNotBeforeNanos) {
				return cache.value;
			}
			List<DeployableArtifact> rebuilt = Collections.unmodifiableList(computeWorkspaceDeployables());
			workspaceDeployablesCache = new CacheValue<>(generation, now, rebuilt);
			return rebuilt;
		}
	}

	private List<DeployableArtifact> computeWorkspaceDeployables() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		if (projects == null || projects.length == 0) {
			return Collections.emptyList();
		}
		List<DeployableArtifact> result = new ArrayList<>();
		for (IProject project : projects) {
			if (project == null || !project.exists() || !project.isOpen()) {
				continue;
			}
			IPath location = project.getLocation();
			Path projectPath = location == null ? null : location.toFile().toPath();
			IModule[] modules = ServerUtil.getModules(project);
			for (IModule module : modules) {
				String label = ServerUtil.getModuleDisplayName(module);
				String typeId = module.getModuleType() == null ? null : module.getModuleType().getId();
				result.add(new DeployableArtifact(null, project.getName(), label, projectPath, typeId));
			}
		}
		return result;
	}

	private IStatus errorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_ID, message, t);
	}

	private IProject resolveProject(Path projectPath, String projectName) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		if (projectName != null && !projectName.isEmpty()) {
			IProject project = getProjectByName(projectName);
			if (project != null && project.exists()) {
				return project;
			}
		}
		if (projectPath == null) {
			return null;
		}
		Path normalized = projectPath.toAbsolutePath().normalize();
		IProject bestMatch = null;
		int bestSegments = -1;
		for (IProject project : root.getProjects()) {
			IPath location = project.getLocation();
			if (location == null) {
				continue;
			}
			Path projectLocation = location.toFile().toPath().toAbsolutePath().normalize();
			if (!normalized.startsWith(projectLocation)) {
				continue;
			}
			int segments = projectLocation.getNameCount();
			if (segments > bestSegments) {
				bestMatch = project;
				bestSegments = segments;
			}
		}
		return bestMatch;
	}

	private IProject getProjectByName(String projectName) {
		if (projectName == null || projectName.isEmpty()) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

	private Collection<Path> getWorkspaceRootsSnapshot() {
		synchronized (workspaceRoots) {
			return new ArrayList<>(workspaceRoots);
		}
	}

	private void onWorkspaceChanged(IResourceChangeEvent event) {
		if (shouldInvalidateWorkspaceDeployables(event)) {
			invalidateWorkspaceDeployablesCache();
		}
	}

	private boolean shouldInvalidateWorkspaceDeployables(IResourceChangeEvent event) {
		if (event == null) {
			return false;
		}
		IResourceDelta delta = event.getDelta();
		if (delta == null) {
			return true;
		}
		final boolean[] changed = new boolean[1];
		try {
			delta.accept(current -> {
				if (changed[0]) {
					return false;
				}
				if (isInvalidatingDelta(current)) {
					changed[0] = true;
					return false;
				}
				return true;
			});
		} catch (CoreException ce) {
			return true;
		}
		return changed[0];
	}

	private boolean isInvalidatingDelta(IResourceDelta delta) {
		if (delta == null) {
			return false;
		}
		IResource resource = delta.getResource();
		if (resource == null) {
			return false;
		}
		if (resource.getType() == IResource.PROJECT) {
			if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
				return true;
			}
			int flags = delta.getFlags();
			return (flags & (IResourceDelta.OPEN | IResourceDelta.TYPE | IResourceDelta.REPLACED)) != 0;
		}
		if (resource.getType() == IResource.FILE) {
			String name = resource.getName();
			return ".project".equals(name)
					|| ".classpath".equals(name)
					|| "org.eclipse.wst.common.component".equals(name)
					|| "org.eclipse.wst.common.project.facet.core.xml".equals(name);
		}
		return false;
	}

	private void invalidateWorkspaceDeployablesCache() {
		synchronized (workspaceDeployablesCacheLock) {
			workspaceDeployablesGeneration++;
			workspaceDeployablesRebuildNotBeforeNanos = System.nanoTime() + REBUILD_DEBOUNCE_NANOS;
		}
	}

	private <T> boolean isCacheFresh(CacheValue<T> cache, long generation, long now) {
		return cache != null
				&& cache.generation == generation
				&& (now - cache.createdAtNanos) <= WORKSPACE_DEPLOYABLES_TTL_NANOS;
	}

	private Collection<Path> normalizeRoots(Collection<Path> roots) {
		if (roots == null || roots.isEmpty()) {
			return Collections.emptyList();
		}
		List<Path> normalized = new ArrayList<>();
		for (Path root : roots) {
			if (root == null) {
				continue;
			}
			normalized.add(root.toAbsolutePath().normalize());
		}
		return normalized;
	}

	private boolean isContainedInAny(Path candidate, Collection<Path> roots) {
		for (Path root : roots) {
			if (root != null && candidate.startsWith(root)) {
				return true;
			}
		}
		return false;
	}

	private static final class CacheValue<T> {
		private final long generation;
		private final long createdAtNanos;
		private final List<T> value;

		private CacheValue(long generation, long createdAtNanos, List<T> value) {
			this.generation = generation;
			this.createdAtNanos = createdAtNanos;
			this.value = value;
		}
	}
}
