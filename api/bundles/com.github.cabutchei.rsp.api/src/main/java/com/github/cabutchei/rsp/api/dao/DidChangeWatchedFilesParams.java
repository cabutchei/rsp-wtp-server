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

public class DidChangeWatchedFilesParams {
	private List<FileEvent> changes;

	public DidChangeWatchedFilesParams() {
	}

	public DidChangeWatchedFilesParams(List<FileEvent> changes) {
		this.changes = changes;
	}

	public List<FileEvent> getChanges() {
		return changes;
	}

	public void setChanges(List<FileEvent> changes) {
		this.changes = changes;
	}
}
