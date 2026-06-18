/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.api.dao;

import com.github.cabutchei.rsp.api.dao.util.Optional;

public class ExportEarRequest {
	private String path;

	@Optional
	private String projectName;

	private String destinationPath;
	private boolean exportSource;

	public ExportEarRequest() {
	}

	public ExportEarRequest(String path, String projectName, String destinationPath, boolean exportSource) {
		this.path = path;
		this.projectName = projectName;
		this.destinationPath = destinationPath;
		this.exportSource = exportSource;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getDestinationPath() {
		return destinationPath;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	public boolean isExportSource() {
		return exportSource;
	}

	public void setExportSource(boolean exportSource) {
		this.exportSource = exportSource;
	}
}
