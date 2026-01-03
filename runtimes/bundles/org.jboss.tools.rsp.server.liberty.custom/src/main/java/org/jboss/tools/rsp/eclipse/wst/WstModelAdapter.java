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
import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.eclipse.core.runtime.MultiStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;

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
