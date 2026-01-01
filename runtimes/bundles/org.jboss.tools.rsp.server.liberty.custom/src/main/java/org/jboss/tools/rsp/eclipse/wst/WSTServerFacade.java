package org.jboss.tools.rsp.eclipse.wst;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;



public class WSTServerFacade {

	private final ServerHandleRegistry registry;
	private final WstModelAdapter adapter;

	public WSTServerFacade(ServerHandleRegistry registry) {
		this(registry, new WstModelAdapter());
	}

	public WSTServerFacade(ServerHandleRegistry registry, WstModelAdapter adapter) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.adapter = Objects.requireNonNull(adapter, "adapter");
	}

	public ServerHandleRegistry getRegistry() {
		return registry;
	}

	public WstModelAdapter getAdapter() {
		return adapter;
	}

	public void dispose() {
		registry.clear();
	}

	public void createServer(IServerType serverType, String id, Map<String, Object> attributes) {
		org.eclipse.wst.server.core.IServer wstServer = null; // Replace with actual server creation logic
		if (serverType == null) {
			throw new IllegalArgumentException("serverType cannot be null");
		}
		org.eclipse.core.runtime.IProgressMonitor monitor = new NullProgressMonitor();
		List<org.eclipse.wst.server.core.IServerType> serverTypes = new ArrayList<>();
		for(org.eclipse.wst.server.core.IServerType st : ServerCore.getServerTypes()) serverTypes.add(st);
		org.eclipse.wst.server.core.IServerType wstServerType = serverTypes.stream().filter(st -> st.getId().equals("com.ibm.ws.st.server.wlp")).findFirst().orElseThrow(null);
		org.eclipse.wst.server.core.IRuntimeType wstRuntimeType = wstServerType.getRuntimeType(); // "com.ibm.ws.st.runtime.wlp"
		org.eclipse.wst.server.core.IRuntimeWorkingCopy runtimeWC;
		try {
			runtimeWC = wstRuntimeType.createRuntime((String)null, monitor);
			runtimeWC.setLocation(new org.eclipse.core.runtime.Path((String) attributes.get("server.home.dir")));
			org.eclipse.wst.server.core.IRuntime run;
			run = runtimeWC.save(true, monitor);
			// set same id so we can retrieve the rsp-wst pairs
			org.eclipse.wst.server.core.IServerWorkingCopy server = wstServerType.createServer(id, null, run, monitor);
			// TODO: replace hardcoded attributes
			server.setHost("localhost");
			server.setAttribute("serverName",   "AppSrv01");
			wstServer = server.save(false, monitor);
			registry.register(wstServer, id);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}