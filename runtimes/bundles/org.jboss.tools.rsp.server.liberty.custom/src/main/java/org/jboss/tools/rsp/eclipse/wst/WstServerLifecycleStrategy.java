package org.jboss.tools.rsp.eclipse.wst;



import java.util.Map;
import java.util.Objects;

import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.ServerCoreActivator;
import org.jboss.tools.rsp.server.spi.model.ServerLifecycleStrategy;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;


// TODO: we are probably getting rid of this class
public class WstServerLifecycleStrategy implements ServerLifecycleStrategy {

	private final WSTServerFacade facade;

	public WstServerLifecycleStrategy(WSTServerFacade facade) {
		this.facade = Objects.requireNonNull(facade, "facade");
	}

	public WSTServerFacade getFacade() {
		return facade;
	}

	public void beforeCreate(IServerType type, String id, Map<String, Object> attributes) throws CoreException{
		this.facade.createServer(type, id, attributes);
	}

	public void afterLoad(IServer server) throws CoreException {
		org.eclipse.wst.server.core.IServer wstServer = this.facade.getRegistry().getWst(server.getId());
		if (wstServer == null) {
			IStatus s = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 
					"Unable to find WST server for RSP server with id: " + server.getId());
			throw new CoreException(s);
		}
		this.facade.getRegistry().register(server, server.getId());
		this.facade.getRegistry().register(wstServer, server.getId());
	}
}
