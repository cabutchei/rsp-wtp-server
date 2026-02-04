package com.github.cabutchei.rsp.eclipse.workspace;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectImporter;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EclipseProjectImporter implements IProjectImporter {
	private static final Logger LOG = LoggerFactory.getLogger(EclipseProjectImporter.class);
	private static final String PROJECT_FILE = ".project";

	private final IWorkspaceService workspaceService;

	public EclipseProjectImporter(IWorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	@Override
	public void initializeProjects(Collection<Path> workspaceRoots) {
		importProjects(workspaceRoots);
	}

	@Override
	public void updateWorkspaceFolders(Collection<Path> added, Collection<Path> removed) {
		if (removed != null && !removed.isEmpty()) {
			removeProjects(removed);
		}
		if (added != null && !added.isEmpty()) {
			importProjects(added);
		}
	}

	private void importProjects(Collection<Path> roots) {
		if (workspaceService == null || roots == null || roots.isEmpty()) {
			return;
		}
		List<Path> projectRoots = new ArrayList<>();
		for (Path root : roots) {
			projectRoots.addAll(findProjectsInRoot(root));
		}
		if (projectRoots.isEmpty()) {
			return;
		}
		IStatus status = workspaceService.importProjects(projectRoots);
		if (status != null && !status.isOK()) {
			LOG.warn("Workspace import reported issues: {}", status.getMessage());
		}
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
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
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
}
