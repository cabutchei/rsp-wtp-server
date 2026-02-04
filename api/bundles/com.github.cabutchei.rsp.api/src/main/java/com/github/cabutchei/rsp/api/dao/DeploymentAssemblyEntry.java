/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.api.dao;

public class DeploymentAssemblyEntry {
	private String sourcePath;
	private String deployPath;
	private String sourceKind;
	private String deployKind;

	public DeploymentAssemblyEntry() {
	}

	public DeploymentAssemblyEntry(String sourcePath, String deployPath) {
		this.sourcePath = sourcePath;
		this.deployPath = deployPath;
	}

	public DeploymentAssemblyEntry(String sourcePath, String deployPath, String sourceKind, String deployKind) {
		this.sourcePath = sourcePath;
		this.deployPath = deployPath;
		this.sourceKind = sourceKind;
		this.deployKind = deployKind;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getDeployPath() {
		return deployPath;
	}

	public void setDeployPath(String deployPath) {
		this.deployPath = deployPath;
	}

	public String getSourceKind() {
		return sourceKind;
	}

	public void setSourceKind(String sourceKind) {
		this.sourceKind = sourceKind;
	}

	public String getDeployKind() {
		return deployKind;
	}

	public void setDeployKind(String deployKind) {
		this.deployKind = deployKind;
	}
}
