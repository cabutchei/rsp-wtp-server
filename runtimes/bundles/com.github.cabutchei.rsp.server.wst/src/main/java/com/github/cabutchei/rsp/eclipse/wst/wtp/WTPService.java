package com.github.cabutchei.rsp.eclipse.wst.wtp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;

import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.wst.core.WSTFacade;
import com.github.cabutchei.rsp.server.spi.workspace.DeployableArtifact;
import com.github.cabutchei.rsp.server.spi.workspace.DeploymentAssemblyEntry;
import com.github.cabutchei.rsp.server.spi.workspace.IWTPService;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceProject;

/**
 * Temporary home for WTP/WST-specific workspace operations.
 */
public class WTPService implements IWTPService {
	private static final String BUNDLE_ID = "com.github.cabutchei.rsp.server.wst";

	private final WSTFacade wstFacade;
	private final Set<Path> workspaceRoots = new LinkedHashSet<>();

	public WTPService(WSTFacade wstFacade) {
		this.wstFacade = wstFacade;
	}

	@Override
	public void setWorkspaceRoots(Collection<Path> workspaceRoots) {
		Collection<Path> normalized = normalizeRoots(workspaceRoots);
		synchronized (this.workspaceRoots) {
			this.workspaceRoots.clear();
			this.workspaceRoots.addAll(normalized);
		}
	}

	@Override
	public List<DeployableArtifact> listDeployableResources(ServerHandle server) {
		List<DeployableArtifact> deployables;
		if (wstFacade != null && server != null) {
			deployables = wstFacade.listDeployableResources(server);
		} else {
			deployables = listWorkspaceDeployables();
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
		if (wstFacade == null) {
			return errorStatus("WST facade is unavailable", null);
		}
		return wstFacade.addDeploymentAssemblyEntry(project, entry);
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
		if (wstFacade == null) {
			return errorStatus("WST facade is unavailable", null);
		}
		return wstFacade.removeDeploymentAssemblyEntry(project, entry);
	}

	@Override
	public IStatus ensureFacets(String projectName, List<String> facetIds) {
		return errorStatus("Facet operations are not implemented", null);
	}

	@Override
	public IStatus updateFacets(String projectName, List<String> add, List<String> remove) {
		return errorStatus("Facet operations are not implemented", null);
	}

	private List<DeployableArtifact> listWorkspaceDeployables() {
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
				result.add(new DeployableArtifact(project.getName(), label, projectPath, typeId));
			}
		}
		return Collections.unmodifiableList(result);
	}

	private IStatus errorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_ID, message, t);
	}

	private IProject resolveProject(Path projectPath, String projectName) {
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

	private boolean isContainedInAny(Path candidate, Collection<Path> roots) {
		for (Path root : roots) {
			if (root != null && candidate.startsWith(root)) {
				return true;
			}
		}
		return false;
	}
}
