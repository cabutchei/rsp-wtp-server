/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.spi.workspace;

import java.nio.file.Path;
import java.util.List;

import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;

public interface IWorkspaceService {

	Path getWorkspaceRoot();

	IStatus openWorkspace(Path workspaceRoot);

	IStatus importProject(Path projectRoot);

	IStatus importAllProjects();

	IStatus refreshProject(String projectName);

	List<WorkspaceProject> listProjects();

	List<DeployableArtifact> listDeployables();

	IStatus ensureFacets(String projectName, List<String> facetIds);

	IStatus updateFacets(String projectName, List<String> add, List<String> remove);
}
