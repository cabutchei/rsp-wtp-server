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
import org.jboss.tools.rsp.api.dao.DeploymentAssemblyRequest;
import org.jboss.tools.rsp.api.dao.DeploymentAssemblyResponse;
import org.jboss.tools.rsp.api.dao.DeploymentAssemblyUpdateRequest;
import org.jboss.tools.rsp.api.dao.DidChangeWorkspaceFoldersParams;
import org.jboss.tools.rsp.api.dao.ListDeployableResourcesResponse;
import org.jboss.tools.rsp.api.dao.ListWorkspaceProjectsResponse;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.api.dao.Status;

@JsonSegment("workspace")
public interface WTPServer {
	/**
	 * The `workspace/getDeployableResources` request is sent by the client to
	 * fetch a list of deployable resources from the current workspace.
	 */
	@JsonRequest
	CompletableFuture<ListDeployableResourcesResponse> getDeployableResources(ServerHandle server);

	/**
	 * The `workspace/listWorkspaceProjects` request is sent by the client to
	 * fetch a list of known workspace projects.
	 */
	@JsonRequest
	CompletableFuture<ListWorkspaceProjectsResponse> listWorkspaceProjects();

	/**
	 * The `workspace/listDeploymentAssemblyProjects` request is sent by the client
	 * to fetch a filtered list of projects that can be added to a deployment
	 * assembly.
	 */
	@JsonRequest
	CompletableFuture<ListWorkspaceProjectsResponse> listDeploymentAssemblyProjects(DeploymentAssemblyRequest request);

	/**
	 * The `workspace/getDeploymentAssembly` request is sent by the client to
	 * fetch the deployment assembly mappings for a workspace project.
	 */
	@JsonRequest
	CompletableFuture<DeploymentAssemblyResponse> getDeploymentAssembly(DeploymentAssemblyRequest request);

	/**
	 * The `workspace/addDeploymentAssemblyEntry` request is sent by the client to
	 * add a new deployment assembly entry.
	 */
	@JsonRequest
	CompletableFuture<Status> addDeploymentAssemblyEntry(DeploymentAssemblyUpdateRequest request);

	/**
	 * The `workspace/removeDeploymentAssemblyEntry` request is sent by the client to
	 * remove an existing deployment assembly entry.
	 */
	@JsonRequest
	CompletableFuture<Status> removeDeploymentAssemblyEntry(DeploymentAssemblyUpdateRequest request);

	/**
	 * The `workspace/didChangeWorkspaceFolders` notification is sent by the client
	 * to inform the server about added or removed workspace folders.
	 */
	@JsonNotification
	void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params);
}
