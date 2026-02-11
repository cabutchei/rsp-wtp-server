/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.api.dao;

import java.util.List;

public class ClasspathContainerMapping {
	private String projectName;
	private String projectUri;
	private String containerPath;
	private String description;
	private List<ClasspathContainerEntry> entries;

	public ClasspathContainerMapping() {
	}

	public ClasspathContainerMapping(String projectName, String projectUri, String containerPath,
			String description, List<ClasspathContainerEntry> entries) {
		this.projectName = projectName;
		this.projectUri = projectUri;
		this.containerPath = containerPath;
		this.description = description;
		this.entries = entries;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectUri() {
		return projectUri;
	}

	public void setProjectUri(String projectUri) {
		this.projectUri = projectUri;
	}

	public String getContainerPath() {
		return containerPath;
	}

	public void setContainerPath(String containerPath) {
		this.containerPath = containerPath;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ClasspathContainerEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<ClasspathContainerEntry> entries) {
		this.entries = entries;
	}
}
