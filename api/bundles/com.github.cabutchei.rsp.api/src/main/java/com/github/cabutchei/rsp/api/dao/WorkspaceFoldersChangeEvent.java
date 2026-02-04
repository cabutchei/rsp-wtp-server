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

public class WorkspaceFoldersChangeEvent {
	private List<WorkspaceFolder> added;
	private List<WorkspaceFolder> removed;

	public WorkspaceFoldersChangeEvent() {
	}

	public WorkspaceFoldersChangeEvent(List<WorkspaceFolder> added, List<WorkspaceFolder> removed) {
		this.added = added;
		this.removed = removed;
	}

	public List<WorkspaceFolder> getAdded() {
		return added;
	}

	public void setAdded(List<WorkspaceFolder> added) {
		this.added = added;
	}

	public List<WorkspaceFolder> getRemoved() {
		return removed;
	}

	public void setRemoved(List<WorkspaceFolder> removed) {
		this.removed = removed;
	}
}
