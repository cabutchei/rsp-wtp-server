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

public interface IProjectImporter {
	/**
	 * Initialize projects under the given workspace roots.
	 * 
	 * @param workspaceRoots workspace roots to initialize
	 */
	void initializeProjects(Collection<Path> workspaceRoots);

	/**
	 * Update projects for added and removed workspace roots.
	 * 
	 * @param added newly added workspace roots
	 * @param removed removed workspace roots
	 */
	void updateWorkspaceFolders(Collection<Path> added, Collection<Path> removed);
}
