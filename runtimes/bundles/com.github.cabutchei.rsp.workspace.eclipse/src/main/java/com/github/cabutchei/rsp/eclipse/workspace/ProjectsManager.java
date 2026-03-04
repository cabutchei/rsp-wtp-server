package com.github.cabutchei.rsp.eclipse.workspace;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.MultiStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.workspace.ClasspathContainerEntry;
import com.github.cabutchei.rsp.server.spi.workspace.ClasspathContainerMapping;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectImporter;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.spi.workspace.IWTPService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import com.github.cabutchei.rsp.server.spi.workspace.JreContainerMapping;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceProject;

public class ProjectsManager implements IProjectsManager {
	private static final Logger LOG = LoggerFactory.getLogger(ProjectsManager.class);
	private static final String BUNDLE_ID = "com.github.cabutchei.rsp.workspace.eclipse";
	private static final String PROJECT_FILE = ".project";
	private static final String CLASSPATH_FILE = ".classpath";
	private static final long REBUILD_DEBOUNCE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
	private static final long FALLBACK_RESCAN_NANOS = TimeUnit.MINUTES.toNanos(5);

	private final IWorkspaceService workspaceService;
	private final IWTPService wtpService;
	private final List<IProjectImporter> projectImporters;
	private final Set<Path> workspaceRoots = new LinkedHashSet<>();

	private final Object snapshotLock = new Object();
	private final Map<String, ProjectSnapshot> projectSnapshots = new LinkedHashMap<>();
	private final Set<String> dirtyProjects = new LinkedHashSet<>();
	private boolean fullRescanRequired = true;
	private long dirtyAtNanos = System.nanoTime();
	private long lastRebuildNanos = System.nanoTime();
	private final IResourceChangeListener workspaceChangeListener = this::onWorkspaceChanged;
	private final IElementChangedListener javaModelChangeListener = this::onJavaModelChanged;

	private boolean initialized;
	private volatile boolean disposed;

	public ProjectsManager(IWorkspaceService workspaceService, List<IProjectImporter> projectImporters) {
		this(workspaceService, null, projectImporters);
	}

	public ProjectsManager(IWorkspaceService workspaceService, IWTPService wtpService, List<IProjectImporter> projectImporters) {
		this.workspaceService = workspaceService;
		this.wtpService = wtpService;
		this.projectImporters = projectImporters == null ? Collections.emptyList() : new ArrayList<>(projectImporters);
		IWorkspace workspace = getWorkspace();
		if (workspace != null) {
			workspace.addResourceChangeListener(workspaceChangeListener, IResourceChangeEvent.POST_CHANGE);
		}
		JavaCore.addElementChangedListener(javaModelChangeListener, ElementChangedEvent.POST_CHANGE);
	}

	private IProject getProject(String projectName) {
		if (projectName == null || projectName.isEmpty()) {
			return null;
		}
		IWorkspaceRoot root = getWorkspaceRoot();
		return root == null ? null : root.getProject(projectName);
	}

	@Override
	public IStatus importProject(Path projectRoot) {
		if (projectRoot == null) {
			return errorStatus("Project root cannot be null", null);
		}
		IWorkspace workspace = getWorkspace();
		if (workspace == null) {
			return errorStatus("Workspace is not available", null);
		}
		IPath projectDescriptionPath = new org.eclipse.core.runtime.Path(projectRoot.resolve(PROJECT_FILE).toString());
		try {
			IProjectDescription description = workspace.loadProjectDescription(projectDescriptionPath);
			IProject project = workspace.getRoot().getProject(description.getName());
			NullProgressMonitor monitor = new NullProgressMonitor();
			if (!project.exists()) {
				project.create(description, monitor);
			}
			if (!project.isOpen()) {
				project.open(monitor);
			}
			markProjectDirty(description.getName());
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			return errorStatus("Failed to import project at " + projectRoot, ce);
		}
	}

	@Override
	public IStatus importAllWorkspaceProjects() {
		return importProjects(discoverProjectsUnderRoots(getWorkspaceRootsSnapshot()));
	}

	@Override
	public IStatus importProjects(List<Path> projectRoots) {
		if (projectRoots == null) {
			return errorStatus("Project roots cannot be null", null);
		}
		if (projectRoots.isEmpty()) {
			return Status.OK_STATUS;
		}
		List<IStatus> failures = new ArrayList<>();
		for (Path projectRoot : projectRoots) {
			if (projectRoot == null) {
				failures.add(errorStatus("Project root cannot be null", null));
				continue;
			}
			IStatus status = importProject(projectRoot);
			if (!status.isOK()) {
				failures.add(status);
			}
		}
		return aggregateImportResults(failures);
	}

	@Override
	public IStatus importProjects(Path[] projectRoots) {
		if (projectRoots == null) {
			return errorStatus("Project roots cannot be null", null);
		}
		return importProjects(Arrays.asList(projectRoots));
	}

	@Override
	public IStatus refreshProject(String projectName) {
		if (projectName == null || projectName.isEmpty()) {
			return errorStatus("Project name cannot be null or empty", null);
		}
		IProject project = getProject(projectName);
		if (project == null || !project.exists()) {
			return errorStatus("Project " + projectName + " does not exist", null);
		}
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			if (!project.isOpen()) {
				project.open(monitor);
			}
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			markProjectDirty(projectName);
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			return errorStatus("Failed to refresh project " + projectName, ce);
		}
	}

	@Override
	public void initializeProjects(Collection<Path> workspaceRoots) {
		markAllProjectsDirty();
		Collection<Path> normalizedRoots = normalizeRoots(workspaceRoots);
		synchronized (this.workspaceRoots) {
			this.workspaceRoots.clear();
			this.workspaceRoots.addAll(normalizedRoots);
		}
		if (wtpService != null) {
			wtpService.setWorkspaceRoots(normalizedRoots);
		}
		initialized = true;
		IStatus importStatus = importProjects(discoverProjectsUnderRoots(normalizedRoots));
		if (!importStatus.isOK()) {
			LOG.warn("Workspace import reported issues during initialization: {}", importStatus.getMessage());
		}
		notifyImportersInit(normalizedRoots);
	}

	@Override
	public void updateWorkspaceFolders(Collection<Path> added, Collection<Path> removed) {
		markAllProjectsDirty();
		Collection<Path> addedRoots = normalizeRoots(added);
		Collection<Path> removedRoots = normalizeRoots(removed);
		synchronized (workspaceRoots) {
			workspaceRoots.removeAll(removedRoots);
			workspaceRoots.addAll(addedRoots);
		}
		if (wtpService != null) {
			wtpService.setWorkspaceRoots(getWorkspaceRootsSnapshot());
		}
		removeProjects(removedRoots);
		IStatus importStatus = importProjects(discoverProjectsUnderRoots(addedRoots));
		if (!importStatus.isOK()) {
			LOG.warn("Workspace import reported issues during workspace-folder update: {}", importStatus.getMessage());
		}
		notifyImportersUpdate(addedRoots, removedRoots);
	}

	@Override
	public List<WorkspaceProject> listWorkspaceProjects() {
		ensureSnapshotsUpToDate();
		List<WorkspaceProject> result = new ArrayList<>();
		synchronized (snapshotLock) {
			for (ProjectSnapshot snapshot : projectSnapshots.values()) {
				result.add(snapshot.workspaceProject);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<JreContainerMapping> listNonStandardJreContainers() {
		ensureSnapshotsUpToDate();
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		List<JreContainerMapping> result = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		synchronized (snapshotLock) {
			for (ProjectSnapshot snapshot : projectSnapshots.values()) {
				if (!snapshot.workspaceProject.isOpen()) {
					continue;
				}
				if (!roots.isEmpty() && (snapshot.normalizedLocation == null
						|| !isContainedInAny(snapshot.normalizedLocation, roots))) {
					continue;
				}
				for (JreContainerMapping mapping : snapshot.nonStandardJreContainers) {
					String key = (mapping.getProjectUri() == null ? mapping.getProjectName() : mapping.getProjectUri())
							+ "|" + mapping.getContainerPath();
					if (seen.add(key)) {
						result.add(mapping);
					}
				}
			}
		}
		return result;
	}

	@Override
	public List<ClasspathContainerMapping> listClasspathContainers() {
		ensureSnapshotsUpToDate();
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		List<ClasspathContainerMapping> result = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		synchronized (snapshotLock) {
			for (ProjectSnapshot snapshot : projectSnapshots.values()) {
				if (!snapshot.workspaceProject.isOpen()) {
					continue;
				}
				if (!roots.isEmpty() && (snapshot.normalizedLocation == null
						|| !isContainedInAny(snapshot.normalizedLocation, roots))) {
					continue;
				}
				for (ClasspathContainerMapping mapping : snapshot.classpathContainers) {
					String key = (mapping.getProjectUri() == null ? mapping.getProjectName() : mapping.getProjectUri())
							+ "|" + mapping.getContainerPath();
					if (seen.add(key)) {
						result.add(mapping);
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public IWTPService getWTPService() {
		return wtpService;
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		IWorkspace workspace = getWorkspace();
		if (workspace != null) {
			workspace.removeResourceChangeListener(workspaceChangeListener);
		}
		JavaCore.removeElementChangedListener(javaModelChangeListener);
		synchronized (snapshotLock) {
			projectSnapshots.clear();
			dirtyProjects.clear();
			fullRescanRequired = true;
		}
		if (wtpService != null) {
			wtpService.dispose();
		}
	}

	private IWorkspace getWorkspace() {
		IWorkspace workspace = workspaceService == null ? null : workspaceService.getWorkspace();
		return workspace == null ? ResourcesPlugin.getWorkspace() : workspace;
	}

	private IWorkspaceRoot getWorkspaceRoot() {
		IWorkspace workspace = getWorkspace();
		return workspace == null ? null : workspace.getRoot();
	}

	private void onWorkspaceChanged(IResourceChangeEvent event) {
		if (disposed) {
			return;
		}
		if (event == null || event.getDelta() == null) {
			markAllProjectsDirty();
			return;
		}
		Set<String> changedProjects = new LinkedHashSet<>();
		try {
			event.getDelta().accept(delta -> visitResourceDelta(delta, changedProjects));
		} catch (CoreException ce) {
			markAllProjectsDirty();
			return;
		}
		if (!changedProjects.isEmpty()) {
			markProjectsDirty(changedProjects);
		}
	}

	private boolean visitResourceDelta(IResourceDelta delta, Set<String> changedProjects) {
		if (delta == null) {
			return true;
		}
		IResource resource = delta.getResource();
		if (resource == null) {
			return true;
		}
		if (resource.getType() == IResource.PROJECT) {
			IProject project = (IProject) resource;
			if (project != null && project.getName() != null) {
				if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
					changedProjects.add(project.getName());
				} else {
					int flags = delta.getFlags();
					if ((flags & (IResourceDelta.OPEN | IResourceDelta.TYPE | IResourceDelta.REPLACED)) != 0) {
						changedProjects.add(project.getName());
					}
				}
			}
		} else if (resource.getType() == IResource.FILE) {
			String name = resource.getName();
			if (PROJECT_FILE.equals(name) || CLASSPATH_FILE.equals(name)) {
				IProject project = resource.getProject();
				if (project != null && project.getName() != null) {
					changedProjects.add(project.getName());
				}
			}
		}
		return true;
	}

	private void onJavaModelChanged(ElementChangedEvent event) {
		if (disposed || event == null) {
			return;
		}
		Set<String> changedProjects = new LinkedHashSet<>();
		collectChangedJavaProjects(event.getDelta(), changedProjects);
		if (!changedProjects.isEmpty()) {
			markProjectsDirty(changedProjects);
		}
	}

	private void collectChangedJavaProjects(IJavaElementDelta delta, Set<String> changedProjects) {
		if (delta == null) {
			return;
		}
		IJavaElement element = delta.getElement();
		if (element != null) {
			IJavaProject javaProject = element.getJavaProject();
			if (javaProject != null && javaProject.getProject() != null) {
				changedProjects.add(javaProject.getProject().getName());
			}
		}
		IJavaElementDelta[] children = delta.getAffectedChildren();
		if (children == null) {
			return;
		}
		for (IJavaElementDelta child : children) {
			collectChangedJavaProjects(child, changedProjects);
		}
	}

	private void ensureSnapshotsUpToDate() {
		if (disposed) {
			return;
		}
		long now = System.nanoTime();
		synchronized (snapshotLock) {
			boolean hasDirty = fullRescanRequired || !dirtyProjects.isEmpty();
			boolean fallbackExpired = (now - lastRebuildNanos) >= FALLBACK_RESCAN_NANOS;
			if (!hasDirty && !fallbackExpired) {
				return;
			}
			if (hasDirty && !fallbackExpired && !projectSnapshots.isEmpty()
					&& (now - dirtyAtNanos) < REBUILD_DEBOUNCE_NANOS) {
				return;
			}
			if (fullRescanRequired || projectSnapshots.isEmpty() || fallbackExpired) {
				rebuildAllProjectSnapshots();
				fullRescanRequired = false;
				dirtyProjects.clear();
			} else {
				refreshDirtyProjectSnapshots();
			}
			lastRebuildNanos = now;
		}
	}

	private void rebuildAllProjectSnapshots() {
		IWorkspaceRoot root = getWorkspaceRoot();
		if (root == null) {
			projectSnapshots.clear();
			return;
		}
		Map<String, ProjectSnapshot> rebuilt = new LinkedHashMap<>();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			ProjectSnapshot snapshot = buildProjectSnapshot(project);
			if (snapshot != null) {
				rebuilt.put(snapshot.workspaceProject.getName(), snapshot);
			}
		}
		projectSnapshots.clear();
		projectSnapshots.putAll(rebuilt);
	}

	private void refreshDirtyProjectSnapshots() {
		List<String> dirty = new ArrayList<>(dirtyProjects);
		dirtyProjects.clear();
		for (String projectName : dirty) {
			refreshProjectSnapshot(projectName);
		}
	}

	private void refreshProjectSnapshot(String projectName) {
		if (projectName == null || projectName.isEmpty()) {
			return;
		}
		IProject project = getProject(projectName);
		if (project == null || !project.exists()) {
			projectSnapshots.remove(projectName);
			return;
		}
		ProjectSnapshot snapshot = buildProjectSnapshot(project);
		if (snapshot == null) {
			projectSnapshots.remove(projectName);
		} else {
			projectSnapshots.put(projectName, snapshot);
		}
	}

	private ProjectSnapshot buildProjectSnapshot(IProject project) {
		if (project == null || !project.exists()) {
			return null;
		}
		String projectName = project.getName();
		IPath location = project.getLocation();
		Path locationPath = location == null ? null : location.toFile().toPath();
		Path normalizedLocation = locationPath == null ? null : locationPath.toAbsolutePath().normalize();
		boolean open = project.isOpen();
		WorkspaceProject workspaceProject = new WorkspaceProject(projectName, locationPath, open);
		if (!open) {
			return new ProjectSnapshot(workspaceProject, normalizedLocation, Collections.emptyList(), Collections.emptyList());
		}
		ProjectContainerMappings mappings = scanProjectContainers(project);
		return new ProjectSnapshot(workspaceProject, normalizedLocation,
				Collections.unmodifiableList(mappings.nonStandardJreContainers),
				Collections.unmodifiableList(mappings.classpathContainers));
	}

	private ProjectContainerMappings scanProjectContainers(IProject project) {
		ProjectContainerMappings mappings = new ProjectContainerMappings();
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null) {
			return mappings;
		}
		IClasspathEntry[] rawEntries;
		try {
			rawEntries = javaProject.getRawClasspath();
		} catch (JavaModelException e) {
			return mappings;
		}
		if (rawEntries == null || rawEntries.length == 0) {
			return mappings;
		}

		String projectUri = null;
		URI locationUri = project.getLocationURI();
		if (locationUri != null) {
			projectUri = locationUri.toString();
		}
		String projectKey = projectUri == null ? project.getName() : projectUri;
		Set<String> seenJre = new HashSet<>();
		Set<String> seenClasspath = new HashSet<>();

		for (IClasspathEntry rawEntry : rawEntries) {
			if (rawEntry == null || rawEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
				continue;
			}
			IPath containerPath = rawEntry.getPath();
			if (containerPath == null) {
				continue;
			}

			if (isNonStandardJreContainer(containerPath)) {
				IVMInstall vm = JavaRuntime.getVMInstall(containerPath);
				if (vm != null && vm.getInstallLocation() != null) {
					String key = projectKey + "|" + containerPath.toString();
					if (seenJre.add(key)) {
						mappings.nonStandardJreContainers.add(new JreContainerMapping(
								project.getName(),
								projectUri,
								containerPath.toString(),
								vm.getName(),
								vm.getInstallLocation().toPath()));
					}
				}
			}

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

			String key = projectKey + "|" + containerPath.toString();
			if (!seenClasspath.add(key)) {
				continue;
			}

			List<ClasspathContainerEntry> containerEntries = new ArrayList<>();
			IClasspathEntry[] children = container.getClasspathEntries();
			if (children != null) {
				for (IClasspathEntry containerEntry : children) {
					if (containerEntry == null) {
						continue;
					}
					String entryPath = resolveEntryPath(containerEntry);
					String sourcePath = resolveEntrySourcePath(containerEntry);
					String javadoc = extractJavadoc(containerEntry);
					containerEntries.add(new ClasspathContainerEntry(
							containerEntry.getEntryKind(),
							entryPath,
							sourcePath,
							stringValue(containerEntry.getSourceAttachmentRootPath()),
							javadoc,
							containerEntry.isExported()));
				}
			}
			mappings.classpathContainers.add(new ClasspathContainerMapping(
					project.getName(),
					projectUri,
					containerPath.toString(),
					container.getDescription(),
					containerEntries));
		}
		return mappings;
	}

	private void markAllProjectsDirty() {
		synchronized (snapshotLock) {
			fullRescanRequired = true;
			dirtyProjects.clear();
			dirtyAtNanos = System.nanoTime();
		}
	}

	private void markProjectDirty(String projectName) {
		if (projectName == null || projectName.isEmpty()) {
			return;
		}
		synchronized (snapshotLock) {
			dirtyProjects.add(projectName);
			dirtyAtNanos = System.nanoTime();
		}
	}

	private void markProjectsDirty(Collection<String> projectNames) {
		if (projectNames == null || projectNames.isEmpty()) {
			return;
		}
		synchronized (snapshotLock) {
			for (String projectName : projectNames) {
				if (projectName != null && !projectName.isEmpty()) {
					dirtyProjects.add(projectName);
				}
			}
			dirtyAtNanos = System.nanoTime();
		}
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

	private List<Path> discoverProjectsUnderRoots(Collection<Path> roots) {
		if (roots == null || roots.isEmpty()) {
			return Collections.emptyList();
		}
		List<Path> results = new ArrayList<>();
		for (Path root : roots) {
			results.addAll(findProjectsInRoot(root));
		}
		return results;
	}

	private List<Path> findProjectsInRoot(Path root) {
		if (root == null || !Files.isDirectory(root)) {
			return Collections.emptyList();
		}
		List<Path> results = new ArrayList<>();
		Path projectFile = root.resolve(PROJECT_FILE);
		if (Files.isRegularFile(projectFile)) {
			results.add(root);
			return results;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
			for (Path child : stream) {
				if (!Files.isDirectory(child)) {
					continue;
				}
				if (Files.isRegularFile(child.resolve(PROJECT_FILE))) {
					results.add(child);
				}
			}
		} catch (IOException ioe) {
			LOG.warn("Failed to scan workspace root {}", root, ioe);
		}
		return results;
	}

	private void removeProjects(Collection<Path> removedRoots) {
		if (removedRoots == null || removedRoots.isEmpty()) {
			return;
		}
		IWorkspaceRoot root = getWorkspaceRoot();
		if (root == null) {
			return;
		}
		IProject[] projects = root.getProjects();
		NullProgressMonitor monitor = new NullProgressMonitor();
		for (IProject project : projects) {
			IPath location = project.getLocation();
			Path projectPath = location == null ? null : location.toFile().toPath();
			if (projectPath == null) {
				continue;
			}
			Path normalized = projectPath.toAbsolutePath().normalize();
			if (!isContainedInAny(normalized, removedRoots)) {
				continue;
			}
			try {
				project.delete(false, true, monitor);
				markProjectDirty(project.getName());
			} catch (CoreException ce) {
				LOG.warn("Failed to remove project {} from workspace", project.getName(), ce);
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

	private IStatus errorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_ID, message, t);
	}

	private IStatus aggregateImportResults(List<IStatus> failures) {
		if (failures.isEmpty()) {
			return Status.OK_STATUS;
		}
		return new MultiStatus(BUNDLE_ID, IStatus.ERROR,
				failures.toArray(new IStatus[0]), "One or more projects failed to import", null);
	}

	private static final class ProjectContainerMappings {
		private final List<JreContainerMapping> nonStandardJreContainers = new ArrayList<>();
		private final List<ClasspathContainerMapping> classpathContainers = new ArrayList<>();
	}

	private static final class ProjectSnapshot {
		private final WorkspaceProject workspaceProject;
		private final Path normalizedLocation;
		private final List<JreContainerMapping> nonStandardJreContainers;
		private final List<ClasspathContainerMapping> classpathContainers;

		private ProjectSnapshot(WorkspaceProject workspaceProject, Path normalizedLocation,
				List<JreContainerMapping> nonStandardJreContainers,
				List<ClasspathContainerMapping> classpathContainers) {
			this.workspaceProject = workspaceProject;
			this.normalizedLocation = normalizedLocation;
			this.nonStandardJreContainers = nonStandardJreContainers;
			this.classpathContainers = classpathContainers;
		}
	}
}
