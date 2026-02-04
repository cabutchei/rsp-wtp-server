/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

public class DeploymentAssemblyEntry {
	private final String sourcePath;
	private final String deployPath;
	private final String sourceKind;
	private final String deployKind;

	public DeploymentAssemblyEntry(String sourcePath, String deployPath) {
		this(sourcePath, deployPath, null, null);
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

	public String getDeployPath() {
		return deployPath;
	}

	public String getSourceKind() {
		return sourceKind;
	}

	public String getDeployKind() {
		return deployKind;
	}
}
