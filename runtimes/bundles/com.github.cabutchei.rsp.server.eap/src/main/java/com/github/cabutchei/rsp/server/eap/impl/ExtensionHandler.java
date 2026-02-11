package com.github.cabutchei.rsp.server.eap.impl;

import com.github.cabutchei.rsp.server.eap.servertype.EapServerTypes;
import com.github.cabutchei.rsp.server.spi.discovery.IServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;

public final class ExtensionHandler {
	private static final IServerType[] TYPES = {
			EapServerTypes.EAP_70_SERVER_TYPE
	};
	private static IServerBeanTypeProvider beanProvider;

	private ExtensionHandler() {
		// no-op
	}

	public static void addExtensions(IServerManagementModel model) {
		beanProvider = new EapServerBeanTypeProvider();
		model.getServerBeanTypeManager().addTypeProvider(beanProvider);
		model.getServerModel().addServerTypes(TYPES);
	}

	public static void removeExtensions(IServerManagementModel model) {
		model.getServerBeanTypeManager().removeTypeProvider(beanProvider);
		model.getServerModel().removeServerTypes(TYPES);
	}
}
