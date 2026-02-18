/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

/**
 * Client-provided workspace initialization options.
 */
public class WorkspaceInitializationRequest {
	private final Boolean autoBuilding;

	public WorkspaceInitializationRequest(Boolean autoBuilding) {
		this.autoBuilding = autoBuilding;
	}

	public Boolean getAutoBuilding() {
		return autoBuilding;
	}

	public static WorkspaceInitializationRequest empty() {
		return new WorkspaceInitializationRequest(null);
	}
}
