/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.github.cabutchei.rsp.api.dao.ServerHandle;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;

public interface IProjectsManager {
	/**
	 * Initialize the workspace projects from the provided workspace roots.
	 * 
	 * @param workspaceRoots workspace roots to initialize
	 */
	void initializeProjects(Collection<Path> workspaceRoots);

	/**
	 * Update the workspace folders that should be considered part of the client workspace.
	 * 
	 * @param added newly added workspace roots
	 * @param removed removed workspace roots
	 */
	void updateWorkspaceFolders(Collection<Path> added, Collection<Path> removed);

	/**
	 * List deployable resources available in the current workspace.
	 */
	List<DeployableArtifact> listDeployableResources(ServerHandle server);

	/**
	 * List workspace projects.
	 */
	List<WorkspaceProject> listWorkspaceProjects();

	/**
	 * List workspace projects that can be added to the deployment assembly of a
	 * given project (excludes existing references and applies module-specific
	 * filtering).
	 *
	 * @param projectPath absolute path to a project or a path within it
	 * @param projectName optional project name hint
	 */
	List<WorkspaceProject> listDeploymentAssemblyProjects(Path projectPath, String projectName);

	/**
	 * Get deployment assembly mappings for a workspace project.
	 * 
	 * @param projectPath absolute path to a project or a path within it
	 * @param projectName optional project name hint
	 */
	List<DeploymentAssemblyEntry> getDeploymentAssembly(Path projectPath, String projectName);

	IStatus addDeploymentAssemblyEntry(Path projectPath, String projectName,
			DeploymentAssemblyEntry entry);

	IStatus removeDeploymentAssemblyEntry(Path projectPath, String projectName,
			DeploymentAssemblyEntry entry);

	/**
	 * Scan workspace projects for non-standard JRE containers that resolve to a VM
	 * install.
	 */
	List<JreContainerMapping> listNonStandardJreContainers();

	/**
	 * @return true if the workspace has been initialized
	 */
	boolean isInitialized();
}
