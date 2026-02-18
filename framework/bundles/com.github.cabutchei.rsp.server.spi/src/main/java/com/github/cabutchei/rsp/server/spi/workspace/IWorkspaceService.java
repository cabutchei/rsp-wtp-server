/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IProject;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;

public interface IWorkspaceService {

	Path getWorkspaceRoot();

	IStatus openWorkspace(Path workspaceRoot);

	IProject getProject(String projectName);

	IStatus importProject(Path projectRoot);

	IStatus importAllProjects();

	IStatus importAllWorkspaceProjects();

	IStatus importProjects(List<Path> projectRoots);

	IStatus importProjects(Path[] projectRoots);

	IStatus refreshProject(String projectName);

	List<WorkspaceProject> listProjects();
}
