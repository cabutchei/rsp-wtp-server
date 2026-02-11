package com.github.cabutchei.rsp.eclipse.workspace;

import java.nio.file.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.server.spi.workspace.DeployableArtifact;
import com.github.cabutchei.rsp.server.spi.workspace.DeploymentAssemblyEntry;
import com.github.cabutchei.rsp.server.spi.workspace.ClasspathContainerEntry;
import com.github.cabutchei.rsp.server.spi.workspace.ClasspathContainerMapping;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectImporter;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import com.github.cabutchei.rsp.server.spi.workspace.JreContainerMapping;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceProject;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;

import com.github.cabutchei.rsp.eclipse.wst.WSTFacade;



public class ProjectsManager implements IProjectsManager {
	private final IWorkspaceService workspaceService;
	private final WSTFacade wstFacade;
	private final List<IProjectImporter> projectImporters;
	private final Set<Path> workspaceRoots = new LinkedHashSet<>();
	private boolean initialized;

	public ProjectsManager(IWorkspaceService workspaceService, List<IProjectImporter> projectImporters) {
		this(workspaceService, null, projectImporters);
	}

	public ProjectsManager(IWorkspaceService workspaceService, WSTFacade wstFacade, List<IProjectImporter> projectImporters) {
		this.workspaceService = workspaceService;
		this.wstFacade = wstFacade;
		this.projectImporters = projectImporters == null ? Collections.emptyList() : new ArrayList<>(projectImporters);
	}

	@Override
	public void initializeProjects(Collection<Path> workspaceRoots) {
		Collection<Path> normalizedRoots = normalizeRoots(workspaceRoots);
		synchronized (this.workspaceRoots) {
			this.workspaceRoots.clear();
			this.workspaceRoots.addAll(normalizedRoots);
		}
		initialized = true;
		notifyImportersInit(normalizedRoots);
	}

	@Override
	public void updateWorkspaceFolders(Collection<Path> added, Collection<Path> removed) {
		Collection<Path> addedRoots = normalizeRoots(added);
		Collection<Path> removedRoots = normalizeRoots(removed);
		synchronized (workspaceRoots) {
			workspaceRoots.removeAll(removedRoots);
			workspaceRoots.addAll(addedRoots);
		}
		notifyImportersUpdate(addedRoots, removedRoots);
	}

	@Override
	public List<DeployableArtifact> listDeployableResources(ServerHandle server) {
		List<DeployableArtifact> deployables;
		if (wstFacade != null && server != null) {
			deployables = wstFacade.listDeployableResources(server);
		} else if (workspaceService != null) {
			deployables = workspaceService.listDeployables();
		} else {
			deployables = Collections.emptyList();
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
	public List<WorkspaceProject> listWorkspaceProjects() {
		if (workspaceService == null) {
			return Collections.emptyList();
		}
		List<WorkspaceProject> projects = workspaceService.listProjects();
		return projects == null ? Collections.emptyList() : projects;
	}

	@Override
	public List<WorkspaceProject> listDeploymentAssemblyProjects(Path projectPath, String projectName) {
		if (wstFacade == null) {
			return Collections.emptyList();
		}
		List<IProject> projects = wstFacade.listDeploymentAssemblyProjects(projectPath, projectName);
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
		if (workspaceService == null) {
			return null;
		}
		return workspaceService.getDeploymentAssembly(projectPath, projectName);
	}

	@Override
	public IStatus addDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry) {
		if (workspaceService == null) {
			return null;
		}
		return workspaceService.addDeploymentAssemblyEntry(projectPath, projectName, entry);
	}

	@Override
	public IStatus removeDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry) {
		if (workspaceService == null) {
			return null;
		}
		return workspaceService.removeDeploymentAssemblyEntry(projectPath, projectName, entry);
	}

	@Override
	public List<JreContainerMapping> listNonStandardJreContainers() {
		if (workspaceService == null) {
			return Collections.emptyList();
		}
		List<WorkspaceProject> projects = workspaceService.listProjects();
		if (projects == null || projects.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		List<JreContainerMapping> mappings = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (WorkspaceProject projectInfo : projects) {
			if (projectInfo == null || !projectInfo.isOpen()) {
				continue;
			}
			IProject project = workspaceService.getProject(projectInfo.getName());
			if (project == null || !project.exists() || !project.isOpen()) {
				continue;
			}
			IPath location = project.getLocation();
			Path projectPath = location == null ? null : location.toFile().toPath().toAbsolutePath().normalize();
			if (!roots.isEmpty() && (projectPath == null || !isContainedInAny(projectPath, roots))) {
				continue;
			}
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject == null) {
				continue;
			}
			IClasspathEntry[] entries;
			try {
				entries = javaProject.getRawClasspath();
			} catch (JavaModelException e) {
				continue;
			}
			if (entries == null) {
				continue;
			}
			for (IClasspathEntry entry : entries) {
				if (entry == null || entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
					continue;
				}
				IPath containerPath = entry.getPath();
				if (!isNonStandardJreContainer(containerPath)) {
					continue;
				}
				IVMInstall vm = JavaRuntime.getVMInstall(containerPath);
				if (vm == null || vm.getInstallLocation() == null) {
					continue;
				}
				String projectUri = null;
				URI locationUri = project.getLocationURI();
				if (locationUri != null) {
					projectUri = locationUri.toString();
				}
				Path javaHome = vm.getInstallLocation().toPath();
				String key = (projectUri == null ? project.getName() : projectUri) + "|" + containerPath.toString();
				if (!seen.add(key)) {
					continue;
				}
				mappings.add(new JreContainerMapping(
						project.getName(),
						projectUri,
						containerPath.toString(),
						vm.getName(),
						javaHome));
			}
		}
		return mappings;
	}

	@Override
	public List<ClasspathContainerMapping> listClasspathContainers() {
		if (workspaceService == null) {
			return Collections.emptyList();
		}
		List<WorkspaceProject> projects = workspaceService.listProjects();
		if (projects == null || projects.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		List<ClasspathContainerMapping> mappings = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (WorkspaceProject projectInfo : projects) {
			if (projectInfo == null || !projectInfo.isOpen()) {
				continue;
			}
			IProject project = workspaceService.getProject(projectInfo.getName());
			if (project == null || !project.exists() || !project.isOpen()) {
				continue;
			}
			IPath location = project.getLocation();
			Path projectPath = location == null ? null : location.toFile().toPath().toAbsolutePath().normalize();
			if (!roots.isEmpty() && (projectPath == null || !isContainedInAny(projectPath, roots))) {
				continue;
			}
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject == null) {
				continue;
			}
			IClasspathEntry[] rawEntries;
			try {
				rawEntries = javaProject.getRawClasspath();
			} catch (JavaModelException e) {
				continue;
			}
			if (rawEntries == null) {
				continue;
			}
			for (IClasspathEntry rawEntry : rawEntries) {
				if (rawEntry == null || rawEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
					continue;
				}
				IPath containerPath = rawEntry.getPath();
				if (isJreContainer(containerPath)) {
					continue;
				}
				IClasspathContainer container;
				try {
					container = JavaCore.getClasspathContainer(containerPath, javaProject);
				} catch (JavaModelException e) {
					continue;
				}
				if (container == null) {
					continue;
				}
				String projectUri = null;
				URI locationUri = project.getLocationURI();
				if (locationUri != null) {
					projectUri = locationUri.toString();
				}
				String key = (projectUri == null ? project.getName() : projectUri) + "|" + containerPath.toString();
				if (!seen.add(key)) {
					continue;
				}
				List<ClasspathContainerEntry> entryMappings = new ArrayList<>();
				IClasspathEntry[] containerEntries = container.getClasspathEntries();
				if (containerEntries != null) {
					for (IClasspathEntry containerEntry : containerEntries) {
						if (containerEntry == null) {
							continue;
						}
						String entryPath = resolveEntryPath(containerEntry);
						String sourcePath = resolveEntrySourcePath(containerEntry);
						String javadoc = extractJavadoc(containerEntry);
						entryMappings.add(new ClasspathContainerEntry(
								containerEntry.getEntryKind(),
								entryPath,
								sourcePath,
								stringValue(containerEntry.getSourceAttachmentRootPath()),
								javadoc,
								containerEntry.isExported()));
					}
				}
				mappings.add(new ClasspathContainerMapping(
						project.getName(),
						projectUri,
						containerPath.toString(),
						container.getDescription(),
						entryMappings));
			}
		}
		return mappings;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	private Collection<Path> getWorkspaceRootsSnapshot() {
		synchronized (workspaceRoots) {
			return new ArrayList<>(workspaceRoots);
		}
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

	private void notifyImportersInit(Collection<Path> roots) {
		if (roots.isEmpty() || projectImporters.isEmpty()) {
			return;
		}
		for (IProjectImporter importer : projectImporters) {
			if (importer != null) {
				importer.initializeProjects(roots);
			}
		}
	}

	private void notifyImportersUpdate(Collection<Path> added, Collection<Path> removed) {
		if ((added == null || added.isEmpty()) && (removed == null || removed.isEmpty())) {
			return;
		}
		if (projectImporters.isEmpty()) {
			return;
		}
		for (IProjectImporter importer : projectImporters) {
			if (importer != null) {
				importer.updateWorkspaceFolders(added, removed);
			}
		}
	}

	private boolean isContainedInAny(Path candidate, Collection<Path> roots) {
		for (Path root : roots) {
			if (root != null && candidate.startsWith(root)) {
				return true;
			}
		}
		return false;
	}

	private boolean isNonStandardJreContainer(IPath containerPath) {
		if (containerPath == null || containerPath.segmentCount() < 1) {
			return false;
		}
		String jreContainerId = String.valueOf(JavaRuntime.JRE_CONTAINER);
		if (!jreContainerId.equals(containerPath.segment(0))) {
			return false;
		}
		if (containerPath.segmentCount() < 2) {
			return false;
		}
		String execEnv = JavaRuntime.getExecutionEnvironmentId(containerPath);
		return execEnv == null || execEnv.trim().isEmpty();
	}

	private boolean isJreContainer(IPath containerPath) {
		if (containerPath == null || containerPath.segmentCount() < 1) {
			return false;
		}
		String jreContainerId = String.valueOf(JavaRuntime.JRE_CONTAINER);
		return jreContainerId.equals(containerPath.segment(0));
	}

	private String extractJavadoc(IClasspathEntry entry) {
		IClasspathAttribute[] attrs = entry.getExtraAttributes();
		if (attrs == null) {
			return null;
		}
		for (IClasspathAttribute attr : attrs) {
			if (attr != null && IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attr.getName())) {
				String value = attr.getValue();
				return value == null || value.isBlank() ? null : value;
			}
		}
		return null;
	}

	private String stringValue(IPath path) {
		return path == null ? null : path.toString();
	}

	private String resolveEntryPath(IClasspathEntry entry) {
		return resolveEntryPath(entry, entry == null ? null : entry.getPath());
	}

	private String resolveEntrySourcePath(IClasspathEntry entry) {
		return resolveEntryPath(entry, entry == null ? null : entry.getSourceAttachmentPath());
	}

	private String resolveEntryPath(IClasspathEntry entry, IPath path) {
		if (path == null || entry == null) {
			return null;
		}
		if (entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY) {
			return path.toString();
		}
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource != null && resource.getLocation() != null) {
			return resource.getLocation().toString();
		}
		return path.toString();
	}
}
