package com.github.cabutchei.rsp.server.websphere.beans;

import com.github.cabutchei.rsp.server.spi.discovery.IServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.spi.discovery.ServerBeanType;

public class WebSphereServerBeanTypeProvider implements IServerBeanTypeProvider {
	private static final ServerBeanType[] TYPES = new ServerBeanType[] {
			new WebSphereServerBeanType()
	};

	@Override
	public ServerBeanType[] getServerBeanTypes() {
		return TYPES;
	}
}
