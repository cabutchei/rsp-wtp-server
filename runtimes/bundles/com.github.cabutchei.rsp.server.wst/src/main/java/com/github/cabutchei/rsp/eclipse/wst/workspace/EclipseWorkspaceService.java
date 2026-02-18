package com.github.cabutchei.rsp.eclipse.wst.workspace;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.MultiStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.workspace.DeployableArtifact;
import com.github.cabutchei.rsp.server.spi.workspace.DeploymentAssemblyEntry;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceProject;

import com.github.cabutchei.rsp.eclipse.wst.core.WSTFacade;

// TODO: clean obsolete methods
public class EclipseWorkspaceService implements IWorkspaceService {
	private static final String BUNDLE_ID = "com.github.cabutchei.rsp.eclipse.wst.workspace";
	private WSTFacade wstFacade;

	public void setWstFacade(WSTFacade wstFacade) {
		this.wstFacade = wstFacade;
	}

	@Override
	public java.nio.file.Path getWorkspaceRoot() {
		IPath rootLocation = null;
		if (rootLocation == null) {
			return null;
		}
		return rootLocation.toFile().toPath();
	}

	@Override
	public IStatus openWorkspace(java.nio.file.Path workspaceRoot) {
		if (workspaceRoot == null) {
			return errorStatus("Workspace root cannot be null", null);
		}
		java.nio.file.Path current = getWorkspaceRoot();
		if (current == null) {
			return errorStatus("Workspace root is not available", null);
		}
		if (!current.equals(workspaceRoot)) {
			return errorStatus("Workspace root is already set to " + current, null);
		}
		return Status.OK_STATUS;
	}

	public IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public IWorkspaceRoot getWorkspaceRootResource() {
		return getWorkspace().getRoot();
	}

	@Override
	public IProject getProject(String projectName) {
		return getWorkspaceRootResource().getProject(projectName);
	}

	@Override
	public IStatus importProject(java.nio.file.Path projectRoot) {
		if (projectRoot == null) {
			return errorStatus("Project root cannot be null", null);
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath projectDescriptionPath = new Path(projectRoot.resolve(".project").toString());
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
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			return errorStatus("Failed to import project at " + projectRoot, ce);
		}
	}

	@Override
	public IStatus importAllProjects() {
		java.nio.file.Path workspaceRoot = getWorkspaceRoot();
		if (workspaceRoot == null) {
			return errorStatus("Workspace root is not available", null);
		}
		if (!Files.isDirectory(workspaceRoot)) {
			return errorStatus("Workspace root is not a directory: " + workspaceRoot, null);
		}
		List<IStatus> failures = new ArrayList<>();
		java.nio.file.Path rootProject = workspaceRoot.resolve(".project");
		if (Files.isRegularFile(rootProject)) {
			IStatus status = importProject(workspaceRoot);
			if (!status.isOK()) {
				failures.add(status);
			}
			return aggregateImportResults(failures);
		}
		try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(workspaceRoot)) {
			for (java.nio.file.Path child : stream) {
				if (!Files.isDirectory(child)) {
					continue;
				}
				java.nio.file.Path projectFile = child.resolve(".project");
				if (!Files.isRegularFile(projectFile)) {
					continue;
				}
				IStatus status = importProject(child);
				if (!status.isOK()) {
					failures.add(status);
				}
			}
		} catch (IOException e) {
			return errorStatus("Failed to scan workspace root " + workspaceRoot, e);
		}
		return aggregateImportResults(failures);
	}

	@Override
	public IStatus importAllWorkspaceProjects() {
		/* we're now reading the project paths from a property passed to the process by the client
		Good enough for now, but I feel like they should be passed to the server via json/rpc request as part of
		the initialization process*/
		String[] workspaceProjectsPaths = System.getProperty("com.github.cabutchei.workspace.projects").split(",");
		java.nio.file.Path[] paths = Arrays.asList(workspaceProjectsPaths).stream().map(p -> java.nio.file.Paths.get(p)).toArray(java.nio.file.Path[]::new);
		return importProjects(paths);
	}

	@Override
	public IStatus importProjects(List<java.nio.file.Path> projectRoots) {
		if (projectRoots == null) {
			return errorStatus("Project roots cannot be null", null);
		}
		if (projectRoots.isEmpty()) {
			return Status.OK_STATUS;
		}
		List<IStatus> failures = new ArrayList<>();
		for (java.nio.file.Path projectRoot : projectRoots) {
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
	public IStatus importProjects(java.nio.file.Path[] projectRoots) {
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
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (!project.exists()) {
			return errorStatus("Project " + projectName + " does not exist", null);
		}
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			if (!project.isOpen()) {
				project.open(monitor);
			}
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			return errorStatus("Failed to refresh project " + projectName, ce);
		}
	}

	@Override
	public List<WorkspaceProject> listProjects() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		List<WorkspaceProject> result = new ArrayList<>(projects.length);
		for (IProject project : projects) {
			IPath location = project.getLocation();
			java.nio.file.Path path = location == null ? null : location.toFile().toPath();
			result.add(new WorkspaceProject(project.getName(), path, project.isOpen()));
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<DeployableArtifact> listDeployables() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		List<DeployableArtifact> result = new ArrayList<>();
		for (IProject project : projects) {
			if (!project.isOpen()) {
				continue;
			}
			IPath location = project.getLocation();
			java.nio.file.Path projectPath = location == null ? null : location.toFile().toPath();
			IModule[] modules = ServerUtil.getModules(project);
			for (IModule module : modules) {
				String label = ServerUtil.getModuleDisplayName(module);
				String typeId = module.getModuleType() == null ? null : module.getModuleType().getId();
				result.add(new DeployableArtifact(project.getName(), label, projectPath, typeId));
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<DeploymentAssemblyEntry> getDeploymentAssembly(java.nio.file.Path projectPath, String projectName) {
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
		if (wstFacade == null) {
			return null;
		}
		List<DeploymentAssemblyEntry> entries = wstFacade.getDeploymentAssembly(project);
		return entries == null ? null : Collections.unmodifiableList(entries);
	}

	@Override
	public IStatus addDeploymentAssemblyEntry(java.nio.file.Path projectPath, String projectName,
			DeploymentAssemblyEntry entry) {
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
		if (wstFacade == null) {
			return errorStatus("WST facade is unavailable", null);
		}
		return wstFacade.addDeploymentAssemblyEntry(project, entry);
	}

	@Override
	public IStatus removeDeploymentAssemblyEntry(java.nio.file.Path projectPath, String projectName,
			DeploymentAssemblyEntry entry) {
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
		if (wstFacade == null) {
			return errorStatus("WST facade is unavailable", null);
		}
		return wstFacade.removeDeploymentAssemblyEntry(project, entry);
	}

	@Override
	public IStatus ensureFacets(String projectName, List<String> facetIds) {
		// ResourcesPlugin.getWorkspace().getRoot()
		return errorStatus("Facet operations are not implemented", null);
	}

	@Override
	public IStatus updateFacets(String projectName, List<String> add, List<String> remove) {
		return errorStatus("Facet operations are not implemented", null);
	}

	private IStatus errorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_ID, message, t);
	}

	private IStatus aggregateImportResults(List<IStatus> failures) {
		if (failures.isEmpty()) {
			return Status.OK_STATUS;
		}
		MultiStatus status = new MultiStatus(BUNDLE_ID, IStatus.ERROR,
				failures.toArray(new IStatus[0]), "One or more projects failed to import", null);
		return status;
	}

	private IProject resolveProject(java.nio.file.Path projectPath, String projectName) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		if (projectName != null && !projectName.isEmpty()) {
			IProject project = root.getProject(projectName);
			if (project != null && project.exists()) {
				return project;
			}
		}
		if (projectPath == null) {
			return null;
		}
		java.nio.file.Path normalized = projectPath.toAbsolutePath().normalize();
		IProject bestMatch = null;
		int bestSegments = -1;
		for (IProject project : root.getProjects()) {
			IPath location = project.getLocation();
			if (location == null) {
				continue;
			}
			java.nio.file.Path projectLocation = location.toFile().toPath().toAbsolutePath().normalize();
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
}
