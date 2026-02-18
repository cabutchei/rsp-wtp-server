/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;

/**
 * Applies client-scoped workspace initialization policies.
 */
public interface IWorkspaceInitializationService {
	/**
	 * Apply (or replace) the initialization request for a client.
	 */
	IStatus applyInitialization(String clientId, WorkspaceInitializationRequest request);

	/**
	 * Release a previously-applied request for a client.
	 */
	IStatus releaseInitialization(String clientId);

	/**
	 * Get a snapshot of current requests and effective policy.
	 */
	WorkspaceInitializationSnapshot snapshot();

	/**
	 * Reapply the currently effective policy to the workspace.
	 */
	IStatus reapplyEffectivePolicy();
}
