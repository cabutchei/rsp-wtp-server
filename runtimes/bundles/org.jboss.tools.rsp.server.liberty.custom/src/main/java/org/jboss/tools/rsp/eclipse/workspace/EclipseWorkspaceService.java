/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.spi.workspace.DeployableArtifact;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;
import org.jboss.tools.rsp.server.spi.workspace.WorkspaceProject;

public class EclipseWorkspaceService implements IWorkspaceService {
	private static final String BUNDLE_ID = "org.jboss.tools.rsp.eclipse.workspace";

	@Override
	public java.nio.file.Path getWorkspaceRoot() {
		IPath rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
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
	public IStatus ensureFacets(String projectName, List<String> facetIds) {
		return errorStatus("Facet operations are not implemented", null);
	}

	@Override
	public IStatus updateFacets(String projectName, List<String> add, List<String> remove) {
		return errorStatus("Facet operations are not implemented", null);
	}

	private IStatus errorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_ID, message, t);
	}
}
