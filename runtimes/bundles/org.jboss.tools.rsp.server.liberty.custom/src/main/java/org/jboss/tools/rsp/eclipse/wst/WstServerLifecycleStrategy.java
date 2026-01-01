package org.jboss.tools.rsp.eclipse.wst;

import java.util.Map;
import java.util.Objects;

import org.jboss.tools.rsp.server.spi.model.ServerLifecycleStrategy;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;

public class WstServerLifecycleStrategy implements ServerLifecycleStrategy {

	private final WSTServerFacade facade;

	public WstServerLifecycleStrategy(WSTServerFacade facade) {
		this.facade = Objects.requireNonNull(facade, "facade");
	}

	public WSTServerFacade getFacade() {
		return facade;
	}

	public void beforeCreate(IServerType type, String id, Map<String, Object> attributes) {
		this.facade.createServer(type, id, attributes);
	}
}
