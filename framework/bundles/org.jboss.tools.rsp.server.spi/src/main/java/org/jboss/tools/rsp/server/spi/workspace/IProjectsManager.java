/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.spi.workspace;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

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
	List<DeployableArtifact> listDeployableResources();

	/**
	 * @return true if the workspace has been initialized
	 */
	boolean isInitialized();
}
