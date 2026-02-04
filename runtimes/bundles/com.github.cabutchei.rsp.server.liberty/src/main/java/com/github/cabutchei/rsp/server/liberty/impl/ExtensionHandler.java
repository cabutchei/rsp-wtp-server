package com.github.cabutchei.rsp.server.liberty.impl;

import com.github.cabutchei.rsp.server.liberty.beans.LibertyServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.liberty.servertype.LibertyServerTypes;
import com.github.cabutchei.rsp.server.spi.discovery.IServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;

public class ExtensionHandler {
	private static final IServerType[] TYPES = {
			LibertyServerTypes.LIBERTY_SERVER_TYPE
	};

	private static IServerBeanTypeProvider beanProvider;

	private ExtensionHandler() {
		// no-op
	}

	public static void addExtensions(IServerManagementModel model) {
		beanProvider = new LibertyServerBeanTypeProvider();
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
