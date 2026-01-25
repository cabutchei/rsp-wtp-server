/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.workspace;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.rsp.server.spi.workspace.DeployableArtifact;
import org.jboss.tools.rsp.server.spi.workspace.IProjectsManager;

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
	public List<DeployableArtifact> listDeployableResources() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}
}
