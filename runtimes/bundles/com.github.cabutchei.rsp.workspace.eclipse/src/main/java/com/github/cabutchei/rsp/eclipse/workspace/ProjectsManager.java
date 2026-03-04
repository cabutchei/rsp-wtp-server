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
import java.util.LinkedHashSet;
import java.util.List;
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
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
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
	private static final long CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(15);
	private static final long REBUILD_DEBOUNCE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);

	private final IWorkspaceService workspaceService;
	private final IWTPService wtpService;
	private final List<IProjectImporter> projectImporters;
	private final Set<Path> workspaceRoots = new LinkedHashSet<>();
	private final Object cacheLock = new Object();
	private volatile long cacheGeneration = 1L;
	private volatile long rebuildNotBeforeNanos;
	private volatile CacheValue<WorkspaceProject> workspaceProjectsCache;
	private volatile CacheValue<JreContainerMapping> nonStandardJreContainersCache;
	private volatile CacheValue<ClasspathContainerMapping> classpathContainersCache;
	private final IResourceChangeListener workspaceChangeListener = this::onWorkspaceChanged;
	private boolean initialized;

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
			markCachesDirty();
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
		markCachesDirty();
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
			markCachesDirty();
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			return errorStatus("Failed to refresh project " + projectName, ce);
		}
	}

	@Override
	public void initializeProjects(Collection<Path> workspaceRoots) {
		markCachesDirty();
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
		markCachesDirty();
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
		CacheValue<WorkspaceProject> cache = workspaceProjectsCache;
		long now = System.nanoTime();
		long generation = cacheGeneration;
		if (isCacheFresh(cache, generation, now)) {
			return cache.value;
		}
		synchronized (cacheLock) {
			cache = workspaceProjectsCache;
			now = System.nanoTime();
			generation = cacheGeneration;
			if (isCacheFresh(cache, generation, now)) {
				return cache.value;
			}
			if (cache != null && now < rebuildNotBeforeNanos) {
				return cache.value;
			}
			List<WorkspaceProject> rebuilt = Collections.unmodifiableList(computeWorkspaceProjects());
			workspaceProjectsCache = new CacheValue<>(generation, now, rebuilt);
			return rebuilt;
		}
	}

	@Override
	public List<JreContainerMapping> listNonStandardJreContainers() {
		CacheValue<JreContainerMapping> cache = nonStandardJreContainersCache;
		long now = System.nanoTime();
		long generation = cacheGeneration;
		if (isCacheFresh(cache, generation, now)) {
			return new ArrayList<>(cache.value);
		}
		synchronized (cacheLock) {
			cache = nonStandardJreContainersCache;
			now = System.nanoTime();
			generation = cacheGeneration;
			if (isCacheFresh(cache, generation, now)) {
				return new ArrayList<>(cache.value);
			}
			if (cache != null && now < rebuildNotBeforeNanos) {
				return new ArrayList<>(cache.value);
			}
			List<JreContainerMapping> rebuilt = Collections.unmodifiableList(computeNonStandardJreContainers());
			nonStandardJreContainersCache = new CacheValue<>(generation, now, rebuilt);
			return new ArrayList<>(rebuilt);
		}
	}

	@Override
	public List<ClasspathContainerMapping> listClasspathContainers() {
		CacheValue<ClasspathContainerMapping> cache = classpathContainersCache;
		long now = System.nanoTime();
		long generation = cacheGeneration;
		if (isCacheFresh(cache, generation, now)) {
			return new ArrayList<>(cache.value);
		}
		synchronized (cacheLock) {
			cache = classpathContainersCache;
			now = System.nanoTime();
			generation = cacheGeneration;
			if (isCacheFresh(cache, generation, now)) {
				return new ArrayList<>(cache.value);
			}
			if (cache != null && now < rebuildNotBeforeNanos) {
				return new ArrayList<>(cache.value);
			}
			List<ClasspathContainerMapping> rebuilt = Collections.unmodifiableList(computeClasspathContainers());
			classpathContainersCache = new CacheValue<>(generation, now, rebuilt);
			return new ArrayList<>(rebuilt);
		}
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public IWTPService getWTPService() {
		return wtpService;
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
		if (shouldInvalidateCaches(event)) {
			markCachesDirty();
		}
	}

	private boolean shouldInvalidateCaches(IResourceChangeEvent event) {
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
			return PROJECT_FILE.equals(name) || CLASSPATH_FILE.equals(name);
		}
		return false;
	}

	private void markCachesDirty() {
		synchronized (cacheLock) {
			cacheGeneration++;
			rebuildNotBeforeNanos = System.nanoTime() + REBUILD_DEBOUNCE_NANOS;
		}
	}

	private <T> boolean isCacheFresh(CacheValue<T> cache, long generation, long now) {
		return cache != null
				&& cache.generation == generation
				&& (now - cache.createdAtNanos) <= CACHE_TTL_NANOS;
	}

	private List<WorkspaceProject> computeWorkspaceProjects() {
		IWorkspaceRoot root = getWorkspaceRoot();
		if (root == null) {
			return Collections.emptyList();
		}
		IProject[] projects = root.getProjects();
		List<WorkspaceProject> result = new ArrayList<>(projects.length);
		for (IProject project : projects) {
			IPath location = project.getLocation();
			Path path = location == null ? null : location.toFile().toPath();
			result.add(new WorkspaceProject(project.getName(), path, project.isOpen()));
		}
		return result;
	}

	private List<JreContainerMapping> computeNonStandardJreContainers() {
		List<WorkspaceProject> projects = listWorkspaceProjects();
		if (projects.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		List<JreContainerMapping> mappings = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (WorkspaceProject projectInfo : projects) {
			if (projectInfo == null || !projectInfo.isOpen()) {
				continue;
			}
			IProject project = getProject(projectInfo.getName());
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

	private List<ClasspathContainerMapping> computeClasspathContainers() {
		List<WorkspaceProject> projects = listWorkspaceProjects();
		if (projects.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<Path> roots = getWorkspaceRootsSnapshot();
		List<ClasspathContainerMapping> mappings = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (WorkspaceProject projectInfo : projects) {
			if (projectInfo == null || !projectInfo.isOpen()) {
				continue;
			}
			IProject project = getProject(projectInfo.getName());
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
