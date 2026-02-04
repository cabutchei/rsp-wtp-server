/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.api.dao;

public class JreContainerMapping {
	private String projectName;
	private String projectUri;
	private String containerPath;
	private String vmName;
	private String javaHome;

	public JreContainerMapping() {
	}

	public JreContainerMapping(String projectName, String projectUri, String containerPath,
			String vmName, String javaHome) {
		this.projectName = projectName;
		this.projectUri = projectUri;
		this.containerPath = containerPath;
		this.vmName = vmName;
		this.javaHome = javaHome;
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

	public String getVmName() {
		return vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public String getJavaHome() {
		return javaHome;
	}

	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}
}
