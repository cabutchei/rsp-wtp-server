/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.workspace;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.workspace.DeployableArtifact;
import com.github.cabutchei.rsp.server.spi.workspace.DeploymentAssemblyEntry;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.spi.workspace.JreContainerMapping;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceProject;

public class DefaultProjectsManager implements IProjectsManager {
	private boolean initialized;

	@Override
	public void initializeProjects(Collection<Path> workspaceRoots) {
		initialized = true;
	}

	@Override
	public void updateWorkspaceFolders(Collection<Path> added, Collection<Path> removed) {
		// Default implementation: no-op.
	}

	@Override
	public List<DeployableArtifact> listDeployableResources(ServerHandle server) {
		return Collections.emptyList();
	}

	@Override
	public List<WorkspaceProject> listWorkspaceProjects() {
		return Collections.emptyList();
	}

	@Override
	public List<WorkspaceProject> listDeploymentAssemblyProjects(Path projectPath, String projectName) {
		return Collections.emptyList();
	}

	@Override
	public List<DeploymentAssemblyEntry> getDeploymentAssembly(Path projectPath, String projectName) {
		return null;
	}

	@Override
	public IStatus addDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry) {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus removeDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry) {
		return Status.OK_STATUS;
	}

	@Override
	public List<JreContainerMapping> listNonStandardJreContainers() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}
}
