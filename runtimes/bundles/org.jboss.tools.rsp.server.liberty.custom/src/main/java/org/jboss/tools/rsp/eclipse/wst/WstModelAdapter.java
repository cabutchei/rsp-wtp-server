/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.eclipse.core.runtime.MultiStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.spi.model.IServerModel;

/**
 * Adapts WST/Eclipse runtime objects to the RSP runtime equivalents.
 */
public class WstModelAdapter {
	private static final String UNKNOWN_PLUGIN_ID = "unknown";

	public org.jboss.tools.rsp.eclipse.core.runtime.IStatus toRspStatus(IStatus status) {
		if (status == null) {
			return new Status(Status.ERROR, UNKNOWN_PLUGIN_ID, "WST status was null");
		}
		String pluginId = normalizePluginId(status.getPlugin());
		if (status.isMultiStatus()) {
			org.jboss.tools.rsp.eclipse.core.runtime.IStatus[] children = convertChildren(status.getChildren());
			return new MultiStatus(pluginId, status.getCode(), children, status.getMessage(), status.getException());
		}
		return new Status(status.getSeverity(), pluginId, status.getCode(), status.getMessage(), status.getException());
	}

	public int toRspPublishKind(int wstPublishKind) {
		switch (wstPublishKind) {
			case IServer.PUBLISH_INCREMENTAL:
				return ServerManagementAPIConstants.PUBLISH_INCREMENTAL;
			case IServer.PUBLISH_FULL:
				return ServerManagementAPIConstants.PUBLISH_FULL;
			case IServer.PUBLISH_AUTO:
				return ServerManagementAPIConstants.PUBLISH_AUTO;
			case IServer.PUBLISH_CLEAN:
				return ServerManagementAPIConstants.PUBLISH_CLEAN;
			default:
				return wstPublishKind;
		}
	}

	public int toWstPublishKind(int rspPublishKind) {
		switch (rspPublishKind) {
			case ServerManagementAPIConstants.PUBLISH_INCREMENTAL:
				return IServer.PUBLISH_INCREMENTAL;
			case ServerManagementAPIConstants.PUBLISH_FULL:
				return IServer.PUBLISH_FULL;
			case ServerManagementAPIConstants.PUBLISH_AUTO:
				return IServer.PUBLISH_AUTO;
			case ServerManagementAPIConstants.PUBLISH_CLEAN:
				return IServer.PUBLISH_CLEAN;
			default:
				return rspPublishKind;
		}
	}

	public int toRspPublishState(int wstPublishState) {
		switch (wstPublishState) {
			case IServer.PUBLISH_STATE_NONE:
				return ServerManagementAPIConstants.PUBLISH_STATE_NONE;
			case IServer.PUBLISH_STATE_INCREMENTAL:
				return ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL;
			case IServer.PUBLISH_STATE_FULL:
				return ServerManagementAPIConstants.PUBLISH_STATE_FULL;
			// case IServer.PUBLISH_STATE_ADD:
			// 	return ServerManagementAPIConstants.PUBLISH_STATE_ADD;
			// case IServer.PUBLISH_STATE_REMOVE:
			// 	return ServerManagementAPIConstants.PUBLISH_STATE_REMOVE;
			case IServer.PUBLISH_STATE_UNKNOWN:
				return ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN;
			default:
				return ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN;
		}
	}

	public int toWstPublishState(int rspPublishState) {
		switch (rspPublishState) {
			case ServerManagementAPIConstants.PUBLISH_STATE_NONE:
				return IServer.PUBLISH_STATE_NONE;
			case ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL:
				return IServer.PUBLISH_STATE_INCREMENTAL;
			case ServerManagementAPIConstants.PUBLISH_STATE_FULL:
				return IServer.PUBLISH_STATE_FULL;
			// case ServerManagementAPIConstants.PUBLISH_STATE_ADD:
			// 	return IServer.PUBLISH_STATE_ADD;
			// case ServerManagementAPIConstants.PUBLISH_STATE_REMOVE:
			// 	return IServer.PUBLISH_STATE_REMOVE;
			case ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN:
				return IServer.PUBLISH_STATE_UNKNOWN;
			default:
				return IServer.PUBLISH_STATE_UNKNOWN;
		}
	}

	public int toRspServerState(int wstServerState) {
		switch (wstServerState) {
			case IServer.STATE_STARTING:
				return ServerManagementAPIConstants.STATE_STARTING;
			case IServer.STATE_STARTED:
				return ServerManagementAPIConstants.STATE_STARTED;
			case IServer.STATE_STOPPING:
				return ServerManagementAPIConstants.STATE_STOPPING;
			case IServer.STATE_STOPPED:
				return ServerManagementAPIConstants.STATE_STOPPED;
			case IServer.STATE_UNKNOWN:
				return ServerManagementAPIConstants.STATE_UNKNOWN;
			default:
				return ServerManagementAPIConstants.STATE_UNKNOWN;
		}
	}

	public int toWstServerState(int rspServerState) {
		switch (rspServerState) {
			case ServerManagementAPIConstants.STATE_STARTING:
				return IServer.STATE_STARTING;
			case ServerManagementAPIConstants.STATE_STARTED:
				return IServer.STATE_STARTED;
			case ServerManagementAPIConstants.STATE_STOPPING:
				return IServer.STATE_STOPPING;
			case ServerManagementAPIConstants.STATE_STOPPED:
				return IServer.STATE_STOPPED;
			case ServerManagementAPIConstants.STATE_UNKNOWN:
				return IServer.STATE_UNKNOWN;
			default:
				return IServer.STATE_UNKNOWN;
		}
	}

	public org.jboss.tools.rsp.server.spi.servertype.IServerType toRspServerType(IServerType wstType, IServerModel model) {
		if (wstType == null || model == null) {
			return null;
		}
		return model.getIServerType(wstType.getId());
	}

	private org.jboss.tools.rsp.eclipse.core.runtime.IStatus[] convertChildren(IStatus[] children) {
		if (children == null || children.length == 0) {
			return new org.jboss.tools.rsp.eclipse.core.runtime.IStatus[0];
		}
		org.jboss.tools.rsp.eclipse.core.runtime.IStatus[] converted = new org.jboss.tools.rsp.eclipse.core.runtime.IStatus[children.length];
		for (int i = 0; i < children.length; i++) {
			converted[i] = toRspStatus(children[i]);
		}
		return converted;
	}

	private String normalizePluginId(String pluginId) {
		if (pluginId == null || pluginId.isEmpty()) {
			return UNKNOWN_PLUGIN_ID;
		}
		return pluginId;
	}
}
