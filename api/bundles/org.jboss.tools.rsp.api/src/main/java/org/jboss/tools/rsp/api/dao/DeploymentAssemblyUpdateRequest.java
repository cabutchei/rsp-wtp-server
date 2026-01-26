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

public class DeploymentAssemblyUpdateRequest {
	private String path;

	@Optional
	private String projectName;
	private DeploymentAssemblyEntry entry;

	public DeploymentAssemblyUpdateRequest() {
	}

	public DeploymentAssemblyUpdateRequest(String path, String projectName, DeploymentAssemblyEntry entry) {
		this.path = path;
		this.projectName = projectName;
		this.entry = entry;
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

	public DeploymentAssemblyEntry getEntry() {
		return entry;
	}

	public void setEntry(DeploymentAssemblyEntry entry) {
		this.entry = entry;
	}
}
