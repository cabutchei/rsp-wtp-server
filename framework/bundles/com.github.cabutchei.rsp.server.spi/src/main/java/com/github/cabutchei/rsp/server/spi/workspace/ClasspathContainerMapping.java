/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.server.spi.workspace;

import java.util.List;

public class ClasspathContainerMapping {
	private final String projectName;
	private final String projectUri;
	private final String containerPath;
	private final String description;
	private final List<ClasspathContainerEntry> entries;

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

	public String getProjectUri() {
		return projectUri;
	}

	public String getContainerPath() {
		return containerPath;
	}

	public String getDescription() {
		return description;
	}

	public List<ClasspathContainerEntry> getEntries() {
		return entries;
	}
}
