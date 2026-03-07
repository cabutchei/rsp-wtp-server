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
import java.nio.file.Paths;

import com.github.cabutchei.rsp.api.dao.DidChangeWatchedFilesParams;
import com.github.cabutchei.rsp.api.dao.FileEvent;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;

public class WorkspaceEventsHandler {
	private final IProjectsManager projectsManager;

	public WorkspaceEventsHandler(IProjectsManager projectsManager) {
		this.projectsManager = projectsManager;
	}

	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		if (projectsManager == null || params == null || params.getChanges() == null) {
			return;
		}
		for (FileEvent event : params.getChanges()) {
			if (event == null) {
				continue;
			}
			Path changedPath = toPath(event.getUri());
			if (changedPath == null) {
				continue;
			}
			projectsManager.fileChanged(changedPath, event.getType());
		}
	}

	private Path toPath(String uri) {
		if (uri == null || uri.isBlank()) {
			return null;
		}
		try {
			java.net.URI parsed = new java.net.URI(uri);
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
