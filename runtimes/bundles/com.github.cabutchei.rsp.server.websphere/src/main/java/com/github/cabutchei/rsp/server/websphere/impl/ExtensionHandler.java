package com.github.cabutchei.rsp.server.websphere.impl;

import com.github.cabutchei.rsp.server.spi.discovery.IServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;

import com.github.cabutchei.rsp.server.websphere.beans.WebSphereServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.websphere.servertype.WebSphereServerTypes;



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
