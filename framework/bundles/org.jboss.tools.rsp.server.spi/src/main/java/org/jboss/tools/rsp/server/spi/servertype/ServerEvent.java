/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.spi.servertype;

import java.util.Objects;

import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;

public final class ServerEvent {
	public static final int STATE_CHANGE = 0x0001;
	public static final int PUBLISH_STATE_CHANGE = 0x0002;
	public static final int RESTART_STATE_CHANGE = 0x0004;
	public static final int STATUS_CHANGE = 0x0008;
	public static final int SERVER_CHANGE = 0x0010;
	public static final int MODULE_CHANGE = 0x0020;

	private final IServer server;
	private final int kind;
	private final int state;
	private final int publishState;
	private final boolean restartState;
	private final IStatus status;
	private final DeployableReference[] deployables;

	public ServerEvent(int kind, IServer server, int state, int publishState, boolean restartState) {
		this(kind, server, null, state, publishState, restartState, null);
	}

	public ServerEvent(int kind, IServer server, int state, int publishState, boolean restartState, IStatus status) {
		this(kind, server, null, state, publishState, restartState, status);
	}

	public ServerEvent(int kind, IServer server, DeployableReference[] deployables, int state, int publishState,
			boolean restartState) {
		this(kind, server, deployables, state, publishState, restartState, null);
	}

	public ServerEvent(int kind, IServer server, DeployableReference[] deployables, int state, int publishState,
			boolean restartState, IStatus status) {
		this.server = Objects.requireNonNull(server, "server");
		this.kind = kind;
		this.state = state;
		this.publishState = publishState;
		this.restartState = restartState;
		this.status = status;
		this.deployables = deployables == null ? null : deployables.clone();
	}

	public IServer getServer() {
		return server;
	}

	public int getKind() {
		return kind;
	}

	public int getState() {
		return state;
	}

	public int getPublishState() {
		return publishState;
	}

	public boolean getRestartState() {
		return restartState;
	}

	public IStatus getStatus() {
		return status;
	}

	public DeployableReference[] getDeployables() {
		return deployables == null ? null : deployables.clone();
	}

	public DeployableReference getDeployable() {
		if (deployables == null || deployables.length == 0) {
			return null;
		}
		return deployables[0];
	}
}
