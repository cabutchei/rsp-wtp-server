/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.server.spi.workspace;

import java.nio.file.Path;

public class JreContainerMapping {
	private final String projectName;
	private final String projectUri;
	private final String containerPath;
	private final String vmName;
	private final Path javaHome;

	public JreContainerMapping(String projectName, String projectUri, String containerPath,
			String vmName, Path javaHome) {
		this.projectName = projectName;
		this.projectUri = projectUri;
		this.containerPath = containerPath;
		this.vmName = vmName;
		this.javaHome = javaHome;
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

	public String getVmName() {
		return vmName;
	}

	public Path getJavaHome() {
		return javaHome;
	}
}
