/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.api.dao;

import java.util.List;

public class ListDeployableResourcesResponse {
	private List<DeployableReference> resources;
	private Status status;

	public ListDeployableResourcesResponse() {
	}

	public ListDeployableResourcesResponse(List<DeployableReference> resources, Status status) {
		this.resources = resources;
		this.status = status;
	}

	public List<DeployableReference> getResources() {
		return resources;
	}

	public void setResources(List<DeployableReference> resources) {
		this.resources = resources;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
