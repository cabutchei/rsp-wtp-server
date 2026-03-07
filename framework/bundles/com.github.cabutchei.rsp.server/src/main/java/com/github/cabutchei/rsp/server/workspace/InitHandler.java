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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.cabutchei.rsp.api.dao.InitializeParams;
import com.github.cabutchei.rsp.api.dao.InitializeResult;
import com.github.cabutchei.rsp.api.dao.Status;
import com.github.cabutchei.rsp.api.dao.WorkspaceFolder;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.util.StatusConverter;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.spi.workspace.IWTPConfiguration;

public class InitHandler {
	private static final boolean DEFAULT_AUTO_BUILDING = false;
	private static final boolean DEFAULT_AUTO_PUBLISHING = false;

	private final IServerManagementModel managementModel;
	private final IProjectsManager projectsManager;
	private final AtomicBoolean serversLoaded = new AtomicBoolean(false);

	public InitHandler(IServerManagementModel managementModel, IProjectsManager projectsManager) {
		this.managementModel = managementModel;
		this.projectsManager = projectsManager;
	}

	public InitializeResult initialize(InitializeParams params) {
		if (projectsManager == null) {
			return new InitializeResult(errorStatus("Projects manager unavailable"), Collections.emptyList());
		}
		List<Path> workspaceRoots = toPaths(params == null ? null : params.getWorkspaceFolders());
		projectsManager.initializeProjects(workspaceRoots);
		IStatus autoBuildStatus = configureAutoBuilding();
		if (!autoBuildStatus.isOK()) {
			return new InitializeResult(StatusConverter.convert(autoBuildStatus), projectsManager.getWatchPatterns());
		}
		IStatus autoPublishStatus = configureAutoPublishing();
		if (!autoPublishStatus.isOK()) {
			return new InitializeResult(StatusConverter.convert(autoPublishStatus), projectsManager.getWatchPatterns());
		}
		IStatus loadStatus = ensureServersLoaded();
		if (!loadStatus.isOK()) {
			return new InitializeResult(StatusConverter.convert(loadStatus), projectsManager.getWatchPatterns());
		}
		return new InitializeResult(StatusConverter.convert(com.github.cabutchei.rsp.eclipse.core.runtime.Status.OK_STATUS),
				projectsManager.getWatchPatterns());
	}

	private IStatus configureAutoBuilding() {
		return projectsManager.setAutoBuilding(DEFAULT_AUTO_BUILDING);
	}

	private IStatus configureAutoPublishing() {
		IWTPConfiguration wtpConfiguration = projectsManager.getWTPService();
		if (wtpConfiguration == null) {
			return com.github.cabutchei.rsp.eclipse.core.runtime.Status.OK_STATUS;
		}
		IStatus globalStatus = wtpConfiguration.setGlobalAutoPublishing(DEFAULT_AUTO_PUBLISHING);
		if (globalStatus != null && !globalStatus.isOK()) {
			return globalStatus;
		}
		IStatus perServerStatus = wtpConfiguration.setAutoPublishingForAllServers(DEFAULT_AUTO_PUBLISHING);
		if (perServerStatus != null && !perServerStatus.isOK()) {
			return perServerStatus;
		}
		return com.github.cabutchei.rsp.eclipse.core.runtime.Status.OK_STATUS;
	}

	private IStatus ensureServersLoaded() {
		if (!serversLoaded.compareAndSet(false, true)) {
			return com.github.cabutchei.rsp.eclipse.core.runtime.Status.OK_STATUS;
		}
		try {
			managementModel.getServerModel().loadServers();
			return com.github.cabutchei.rsp.eclipse.core.runtime.Status.OK_STATUS;
		} catch (CoreException e) {
			serversLoaded.set(false);
			return e.getStatus();
		} catch (Exception e) {
			serversLoaded.set(false);
			return new com.github.cabutchei.rsp.eclipse.core.runtime.Status(IStatus.ERROR,
					ServerCoreActivator.BUNDLE_ID, "Failed to load servers", e);
		}
	}

	private List<Path> toPaths(List<WorkspaceFolder> folders) {
		if (folders == null || folders.isEmpty()) {
			return Collections.emptyList();
		}
		List<Path> roots = new ArrayList<>();
		for (WorkspaceFolder folder : folders) {
			Path path = toPath(folder == null ? null : folder.getUri());
			if (path != null) {
				roots.add(path);
			}
		}
		return roots;
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

	private Status errorStatus(String message) {
		return StatusConverter.convert(new com.github.cabutchei.rsp.eclipse.core.runtime.Status(IStatus.ERROR,
				ServerCoreActivator.BUNDLE_ID, message));
	}
}
