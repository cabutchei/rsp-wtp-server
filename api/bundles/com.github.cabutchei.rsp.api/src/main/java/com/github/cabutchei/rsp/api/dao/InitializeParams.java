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

public class InitializeParams {
	private List<WorkspaceFolder> workspaceFolders;

	public InitializeParams() {
	}

	public InitializeParams(List<WorkspaceFolder> workspaceFolders) {
		this.workspaceFolders = workspaceFolders;
	}

	public List<WorkspaceFolder> getWorkspaceFolders() {
		return workspaceFolders;
	}

	public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
		this.workspaceFolders = workspaceFolders;
	}
}
