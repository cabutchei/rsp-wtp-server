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

public class ListWorkspaceProjectsResponse {
	private List<WorkspaceProject> projects;
	private Status status;

	public ListWorkspaceProjectsResponse() {
	}

	public ListWorkspaceProjectsResponse(List<WorkspaceProject> projects, Status status) {
		this.projects = projects;
		this.status = status;
	}

	public List<WorkspaceProject> getProjects() {
		return projects;
	}

	public void setProjects(List<WorkspaceProject> projects) {
		this.projects = projects;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
