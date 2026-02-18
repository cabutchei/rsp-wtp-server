/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;

public interface IWorkspaceService {
	/**
	 * Return the workspace service, or {@code null} if unavailable.
	 */
	IWorkspace getWorkspace();

	/**
	 * Return the workspace root, or {@code null} if unavailable.
	 */
	IWorkspaceRoot getWorkspaceRoot();

	/**
	 * @return whether the workspace service is currently available.
	 */
	boolean isAvailable();

	/**
	 * Complete when a workspace becomes available.
	 */
	CompletableFuture<IWorkspace> whenAvailable();

	/**
	 * Run the given operation once the workspace is available.
	 */
	void whenAvailable(Consumer<IWorkspace> operation);

	/**
	 * Wait for the workspace service to become available.
	 *
	 * @param timeoutMs timeout in milliseconds
	 * @return workspace instance or {@code null} if unavailable before timeout
	 */
	IWorkspace awaitWorkspace(long timeoutMs);
}
