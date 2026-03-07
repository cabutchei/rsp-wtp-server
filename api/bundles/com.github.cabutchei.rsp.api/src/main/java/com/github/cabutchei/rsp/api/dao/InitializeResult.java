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

public class InitializeResult {
	private Status status;
	private List<String> watchPatterns;

	public InitializeResult() {
	}

	public InitializeResult(Status status, List<String> watchPatterns) {
		this.status = status;
		this.watchPatterns = watchPatterns;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public List<String> getWatchPatterns() {
		return watchPatterns;
	}

	public void setWatchPatterns(List<String> watchPatterns) {
		this.watchPatterns = watchPatterns;
	}
}
