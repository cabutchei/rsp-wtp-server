/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.api;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.jboss.tools.rsp.api.dao.DidChangeWorkspaceFoldersParams;
import org.jboss.tools.rsp.api.dao.ListDeployableResourcesResponse;

@JsonSegment("workspace")
public interface WTPServer {
	/**
	 * The `workspace/getDeployableResources` request is sent by the client to
	 * fetch a list of deployable resources from the current workspace.
	 */
	@JsonRequest
	CompletableFuture<ListDeployableResourcesResponse> getDeployableResources();

	/**
	 * The `workspace/didChangeWorkspaceFolders` notification is sent by the client
	 * to inform the server about added or removed workspace folders.
	 */
	@JsonNotification
	void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params);
}
