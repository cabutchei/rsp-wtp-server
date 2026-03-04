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

/**
 * WTP-specific project/deployable operations.
 */
public interface IWTPService {
	/**
	 * Set current workspace roots for filtering workspace-scoped results.
	 */
	void setWorkspaceRoots(Collection<Path> workspaceRoots);

	List<DeployableArtifact> listDeployableResources(ServerHandle server);

	List<WorkspaceProject> listDeploymentAssemblyProjects(Path projectPath, String projectName);

	List<DeploymentAssemblyEntry> getDeploymentAssembly(Path projectPath, String projectName);

	IStatus addDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry);

	IStatus removeDeploymentAssemblyEntry(Path projectPath, String projectName, DeploymentAssemblyEntry entry);

	IStatus ensureFacets(String projectName, List<String> facetIds);

	IStatus updateFacets(String projectName, List<String> add, List<String> remove);

	/**
	 * Dispose listeners/resources associated with this service.
	 */
	default void dispose() {
		// no-op
	}
}
