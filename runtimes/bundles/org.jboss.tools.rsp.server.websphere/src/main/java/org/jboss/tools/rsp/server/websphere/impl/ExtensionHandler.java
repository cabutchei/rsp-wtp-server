/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.websphere.impl;

import org.jboss.tools.rsp.server.spi.discovery.IServerBeanTypeProvider;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;
import org.jboss.tools.rsp.server.websphere.beans.WebSphereServerBeanTypeProvider;
import org.jboss.tools.rsp.server.websphere.servertype.WebSphereServerTypes;

public class ExtensionHandler {
	private static final IServerType[] TYPES = {
			WebSphereServerTypes.WEBSPHERE_SERVER_TYPE
	};

	private static IServerBeanTypeProvider beanProvider;

	private ExtensionHandler() {
		// no-op
	}

	public static void addExtensions(IServerManagementModel model) {
		beanProvider = new WebSphereServerBeanTypeProvider();
		model.getServerBeanTypeManager().addTypeProvider(beanProvider);
		model.getServerModel().addServerTypes(TYPES);
	}

	public static void removeExtensions(IServerManagementModel model) {
		if (beanProvider != null) {
			model.getServerBeanTypeManager().removeTypeProvider(beanProvider);
		}
		model.getServerModel().removeServerTypes(TYPES);
	}
}
