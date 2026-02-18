/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable view of active initialization requests and effective owner/request.
 */
public class WorkspaceInitializationSnapshot {
	private final String ownerClientId;
	private final WorkspaceInitializationRequest effectiveRequest;
	private final Map<String, WorkspaceInitializationRequest> clientRequests;

	public WorkspaceInitializationSnapshot(String ownerClientId,
			WorkspaceInitializationRequest effectiveRequest,
			Map<String, WorkspaceInitializationRequest> clientRequests) {
		this.ownerClientId = ownerClientId;
		this.effectiveRequest = effectiveRequest;
		Map<String, WorkspaceInitializationRequest> safeCopy = clientRequests == null
				? Collections.emptyMap()
				: new LinkedHashMap<>(clientRequests);
		this.clientRequests = Collections.unmodifiableMap(safeCopy);
	}

	public String getOwnerClientId() {
		return ownerClientId;
	}

	public WorkspaceInitializationRequest getEffectiveRequest() {
		return effectiveRequest;
	}

	public Map<String, WorkspaceInitializationRequest> getClientRequests() {
		return clientRequests;
	}
}
