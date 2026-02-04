/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.workspace;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.cabutchei.rsp.api.dao.DidChangeWorkspaceFoldersParams;
import com.github.cabutchei.rsp.api.dao.WorkspaceFolder;
import com.github.cabutchei.rsp.api.dao.WorkspaceFoldersChangeEvent;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;

public class WorkspaceFolderChangeHandler {
	private final IProjectsManager projectsManager;

	public WorkspaceFolderChangeHandler(IProjectsManager projectsManager) {
		this.projectsManager = projectsManager;
	}

	public void update(DidChangeWorkspaceFoldersParams params) {
		if (projectsManager == null || params == null) {
			return;
		}
		WorkspaceFoldersChangeEvent event = params.getEvent();
		if (event == null) {
			return;
		}
		Collection<Path> addedRoots = toPaths(event.getAdded());
		Collection<Path> removedRoots = toPaths(event.getRemoved());
		if (!projectsManager.isInitialized()) {
			projectsManager.initializeProjects(addedRoots);
			return;
		}
		projectsManager.updateWorkspaceFolders(addedRoots, removedRoots);
	}

	private Collection<Path> toPaths(List<WorkspaceFolder> folders) {
		Collection<Path> roots = new ArrayList<>();
		if (folders == null) {
			return roots;
		}
		for (WorkspaceFolder folder : folders) {
			Path path = toPath(folder == null ? null : folder.getUri());
			if (path != null) {
				roots.add(path);
			}
		}
		return roots;
	}

	private Path toPath(String uri) {
		if (uri == null || uri.trim().isEmpty()) {
			return null;
		}
		try {
			URI parsed = new URI(uri);
			if (parsed.getScheme() == null) {
				return Paths.get(uri).toAbsolutePath().normalize();
			}
			if ("file".equalsIgnoreCase(parsed.getScheme())) {
				return Paths.get(parsed).toAbsolutePath().normalize();
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
}
