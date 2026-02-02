package com.github.cabutchei.rsp.server.websphere.beans;

import org.jboss.tools.rsp.server.spi.discovery.IServerBeanTypeProvider;
import org.jboss.tools.rsp.server.spi.discovery.ServerBeanType;

public class WebSphereServerBeanTypeProvider implements IServerBeanTypeProvider {
	private static final ServerBeanType[] TYPES = new ServerBeanType[] {
			new WebSphereServerBeanType()
	};

	@Override
	public ServerBeanType[] getServerBeanTypes() {
		return TYPES;
	}
}
