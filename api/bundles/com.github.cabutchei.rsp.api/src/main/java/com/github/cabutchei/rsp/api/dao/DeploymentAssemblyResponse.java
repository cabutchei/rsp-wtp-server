/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.api.dao;

import java.util.List;

public class DeploymentAssemblyResponse {
	private List<DeploymentAssemblyEntry> entries;
	private Status status;

	public DeploymentAssemblyResponse() {
	}

	public DeploymentAssemblyResponse(List<DeploymentAssemblyEntry> entries, Status status) {
		this.entries = entries;
		this.status = status;
	}

	public List<DeploymentAssemblyEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<DeploymentAssemblyEntry> entries) {
		this.entries = entries;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
