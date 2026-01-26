/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.api.dao;

import org.jboss.tools.rsp.api.dao.util.Optional;

public class DeploymentAssemblyRequest {
	private String path;

	@Optional
	private String projectName;

	public DeploymentAssemblyRequest() {
	}

	public DeploymentAssemblyRequest(String path, String projectName) {
		this.path = path;
		this.projectName = projectName;
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
}
