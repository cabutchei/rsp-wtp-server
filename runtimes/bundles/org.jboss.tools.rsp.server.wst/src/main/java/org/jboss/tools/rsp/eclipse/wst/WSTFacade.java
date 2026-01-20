package org.jboss.tools.rsp.eclipse.wst;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.internal.RuntimeWorkingCopy;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.ServerCoreActivator;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.spi.servertype.IServerListener;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;

import com.ibm.ws.ast.st.common.core.internal.AbstractWASServer;
import com.ibm.ws.ast.st.common.core.internal.util.ProfileChangeHelper;
import com.ibm.ws.ast.st.v85.core.internal.WASServer;

// import com.ibm.ws.st.core.internal.WebSphereRuntime;
// import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
// import com.ibm.ws.ast.st.v85.core.internal.WASServer;



public class WSTFacade {

	private final ServerHandleRegistry registry;
	private final WstModelAdapter adapter;
	private final IWorkspaceService workspaceService;

	public WSTFacade(ServerHandleRegistry registry, IWorkspaceService workspaceService) {
		this(registry, new WstModelAdapter(), workspaceService);
	}

	public WSTFacade(ServerHandleRegistry registry, WstModelAdapter adapter, IWorkspaceService workspaceService) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.adapter = Objects.requireNonNull(adapter, "adapter");
		this.workspaceService = workspaceService;
	}

	public ServerHandleRegistry getRegistry() {
		return registry;
	}

	public WstModelAdapter getAdapter() {
		return adapter;
	}

	public IServer createServerProxy(org.eclipse.wst.server.core.IServer wstServer, IServerManagementModel managementModel) {
		return createServerProxy(wstServer, managementModel, null);
	}

	public IServer createServerProxy(org.eclipse.wst.server.core.IServer wstServer,
			IServerManagementModel managementModel, IServerDelegate delegate) {
		Objects.requireNonNull(wstServer, "wstServer");
		IServerModel serverModel = managementModel.getServerModel();
		IServerType serverType = serverModel.getIServerType(wstServer.getServerType().getId());
		WstServerProxy proxy = new WstServerProxy(wstServer, serverType, managementModel, serverModel, adapter);
		if (delegate != null) {
			proxy.setDelegate(delegate);
		}
		return proxy;
	}

	public IServer[] createServeProxies(IServerManagementModel managementModel) {
		List<IServer> rspServers = new ArrayList<>();
		for (org.eclipse.wst.server.core.IServer wstServer : ServerCore.getServers()) {
			rspServers.add(createServerProxy(wstServer, managementModel));
		}
		return rspServers.toArray(new IServer[rspServers.size()]);
	}

	public void dispose() {
		registry.clear();
	}

	public IStatus addDeployable(DeployableReference ref, ServerHandle server) {
			IProject project = this.workspaceService.getProject(ref.getLabel());
			IModule[] modules = ServerUtil.getModules(project);
			org.eclipse.wst.server.core.IServerWorkingCopy copy = getWstServer(server.getId()).createWorkingCopy();
			try {
				copy.modifyModules(modules, null, new NullProgressMonitor());
				copy.save(false, new NullProgressMonitor());
			} catch (org.eclipse.core.runtime.CoreException e) {
				e.printStackTrace();
				IStatus status = this.adapter.toRspStatus(e.getStatus());
				return status;
			}
			return Status.OK_STATUS;
	}

	public IStatus canAddDeployable(DeployableReference ref, ServerHandle server) {
		IProject project = this.workspaceService.getProject(ref.getLabel());
		if (!project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, NLS.bind("{0} isn't bound to any workspace project", ref.getLabel()));
		}
		org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
		if (modules == null || modules.length == 0) {
			IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
			return status;
		}
		org.eclipse.core.runtime.IStatus status = getWstServer(server.getId()).canModifyModules(modules, null, new NullProgressMonitor());
		return this.adapter.toRspStatus(status);
	}

	public IStatus removeDeployable(DeployableReference ref, ServerHandle server) {
		IProject project = this.workspaceService.getProject(ref.getLabel());
		if (!project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, NLS.bind("{0} isn't bound to any workspace project", ref.getLabel()));
		}
		org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
		if (modules == null || modules.length == 0) {
			IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
			return status;
		}
		org.eclipse.wst.server.core.IServerWorkingCopy copy = getWstServer(server.getId()).createWorkingCopy();
		try {
			copy.modifyModules(null, modules, new NullProgressMonitor());
			copy.save(false, new NullProgressMonitor());
			return Status.OK_STATUS;
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			IStatus status = this.adapter.toRspStatus(e.getStatus());
			return status;
		}
	}

	public IStatus canRemoveDeployable(DeployableReference ref, ServerHandle server) {
		IProject project = this.workspaceService.getProject(ref.getLabel());
		if (!project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, NLS.bind("{0} isn't bound to any workspace project", ref.getLabel()));
		}
		org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
		if (modules == null || modules.length == 0) {
			IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
			return status;
		}
		org.eclipse.core.runtime.IStatus status = getWstServer(server.getId()).canModifyModules(null, modules, new NullProgressMonitor());
		return this.adapter.toRspStatus(status);
	}

	public IStatus canPublish(ServerHandle server) {
		return this.adapter.toRspStatus(getWstServer(server.getId()).canPublish());
	}

	public IStatus publish(ServerHandle handle, int rspKind) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId())
		.publish(this.adapter.toWstPublishKind(rspKind), new NullProgressMonitor());
		return this.adapter.toRspStatus(status);
	}

	public int getServerPublishState(ServerHandle handle) {
		int publishState = getWstServer(handle.getId()).getServerPublishState();
		return this.adapter.toRspPublishState(publishState);
	}

	public int getServerRunState(ServerHandle handle) {
		int runState = getWstServer(handle.getId()).getServerState();
		return this.adapter.toRspServerState(runState);
	}

	private org.eclipse.wst.server.core.IModule getModule(ServerHandle handle, String name) {
		for ( org.eclipse.wst.server.core.IModule module : getWstServer(handle.getId()).getModules()) {
			if (module.getName().equals(name)) {
				return module;
			}
		}
		return null;
	}

	private IModule[] getModules(ServerHandle server) {
		org.eclipse.wst.server.core.IModule[] modules = getWstServer(server.getId()).getModules();
		return modules;
	}

	private int getModulePublishState(ServerHandle server, DeployableReference ref) {
		org.eclipse.wst.server.core.IServer wstServer = getWstServer(server.getId());
		IModule module = getModule(server, ref.getLabel());
		return wstServer.getModulePublishState(new IModule[] {module});
	}

	private int getModuleRunState(ServerHandle server, DeployableReference ref) {
		org.eclipse.wst.server.core.IServer wstServer = getWstServer(server.getId());
		IModule module = getModule(server, ref.getLabel());
		return wstServer.getModuleState(new IModule[] {module});
	}

	public List<DeployableState> getDeployableStates(ServerHandle server) {
		List<DeployableState> states = new ArrayList<>();
		IModule[] modules = getModules(server);
		for (IModule module : modules) {
			// DeployableReference ref = this.adapter.toDeployableReference(module);
			DeployableReference ref = new DeployableReference(module.getName(), module.getProject().getName());
			int runState = this.adapter.toRspServerState(getModuleRunState(server, ref));
			int publishState = this.adapter.toRspPublishState(getModulePublishState(server, ref));
			DeployableState ds = new DeployableState(server, ref, runState, publishState);
			states.add(ds);
		}
		return states;
	}

	public DeployableState getDeployableState(ServerHandle handle, DeployableReference ref) {
		int runState = this.adapter.toRspServerState(getModuleRunState(handle, ref));
		int publishState = this.adapter.toRspPublishState(getModulePublishState(handle, ref));
		DeployableState ds = new DeployableState(handle, ref, runState, publishState);	
		return ds;
	}


	public IServer createServer(IServerType serverType, String id, Map<String, Object> attributes, IServerManagementModel model) throws CoreException {
		org.eclipse.wst.server.core.IServer wstServer = null;
		if (serverType == null) {
			throw new IllegalArgumentException("serverType cannot be null");
		}
		org.eclipse.core.runtime.IProgressMonitor monitor = new NullProgressMonitor();
		org.eclipse.wst.server.core.IServerType wstServerType = this.adapter.toWstServerType(serverType);
		if (wstServerType == null) {
			throw new CoreException(new Status(IStatus.ERROR, "", "Server Type " + serverType.getId() + " is unknown by this Eclipse application"));
		}
		org.eclipse.wst.server.core.IRuntimeType wstRuntimeType = wstServerType.getRuntimeType();
		org.eclipse.wst.server.core.IRuntimeWorkingCopy runtimeWC;
		try {
			runtimeWC = wstRuntimeType.createRuntime((String)null, monitor);
			runtimeWC.setLocation(new org.eclipse.core.runtime.Path((String) attributes.get(ServerManagementAPIConstants.SERVER_HOME_DIR)));
			// TODO: let the user choose the vm?
			// runtimeWC.getAdapter(RuntimeWorkingCopy.class).setAttribute("vm-install-id", "/Users/cabutchei/.sdkman/candidates/java/21.0.2-open");
			// runtimeWC.getAdapter(RuntimeWorkingCopy.class).setAttribute("vm-install-type-id", "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");
			org.eclipse.wst.server.core.IRuntime run = runtimeWC.save(true, monitor);
			// set same id so we can retrieve the rsp-wst pairs
			org.eclipse.wst.server.core.IServerWorkingCopy server = wstServerType.createServer(id, null, run, monitor);
			// TODO: replace hardcoded attributes
			server.setHost("localhost");
			// server.setAttribute("serverName", (String) attributes.get("server.liberty.id"));
			//WebSphere
			server.setAttribute("webSphereProfileName", (String) attributes.get("server.liberty.id"));
			server.setName(id);
			server.setAttribute("id", id);
			WASServer wasServer = ((WASServer) server.loadAdapter(WASServer.class, null));
			String profileName = (String) attributes.get("server.liberty.id");
			if (!Arrays.asList(wasServer.getProfileNames()).contains(profileName)) {
				throw new CoreException(new Status(Status.ERROR, null, "Profile does not exist"));
			}
			wasServer.setWebSphereProfileName(id);
			// this triggers config reading and refresh
			new ProfileChangeHelper().updateBaseServerForProfileChange(server, profileName);
			// TODO: eventually use this to create let the user create a new profile
			// server.getRuntime().getAdapter(com.ibm.ws.st.core.internal.WebSphereRuntime.class).createServer()
			wstServer = server.save(false, monitor);
			return createServerProxy(wstServer, model);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			throw new CoreException(this.adapter.toRspStatus(e.getStatus()));
		}
	}

	public IServer getRspServer(String id) {
		return this.registry.getRsp(id);
	}

	public org.eclipse.wst.server.core.IServer getWstServer(String id) {
		return ServerCore.findServer(id);
		// return getWstServer(id);
	}

	public Map<String, IServer> getServers() {
		return this.registry.getAllRspServers();
	}

	public void addServerListener(String id, IServerListener listener) {
		WstServerProxy server = (WstServerProxy) getRspServer(id);
		if (server == null) {
			throw new IllegalArgumentException("Server does not exist");
		}
		server.addServerListener(listener);
	}


	public void deleteServer(String id) throws CoreException {
		IServer server = getRspServer(id);
		if (server == null) return;
		server.delete();
	}

	// public IServer[] getServers() {
	// 	List<IServer> rspServers = new ArrayList<>();
	// 	for (org.eclipse.wst.server.core.IServer wstServer : ServerCore.getServers()) {
	// 		rspServers.add(createServerProxy(wstServer));
	// 	}
	// 	return rspServers.toArray(new IServer[rspServers.size()]);
	// }

	/** made this because async start caused server ILaunch object to be null. But should we really wait for the done event?
	 * Perhaps it's best if we listen for ILaunch instead?
	 */
	public IStatus startSync(ServerHandle handle, String launchMode) {
		// DebugPlugin.getDefault().getLaunchManager().addLaunchListener(null);
		CompletableFuture<IStatus> future = new CompletableFuture<>();
		IOperationListener listener = (result) -> { future.complete(this.adapter.toRspStatus(result)); };
		getWstServer(handle.getId()).start(launchMode, listener);
		return future.join();
	}

	public void start(ServerHandle handle, String launchMode) throws CoreException {
		try {
			getWstServer(handle.getId()).start(launchMode, (IProgressMonitor) null);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			throw new CoreException(this.adapter.toRspStatus(e.getStatus()));
		}
	}

	public IStatus canStart(ServerHandle handle, String launchMode) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId()).canStart(launchMode);
		return this.adapter.toRspStatus(status);
	}

	public void stop(ServerHandle handle, boolean force) {
		getWstServer(handle.getId()).stop(force);
	}

	public IStatus stopSync(ServerHandle handle, boolean force) {
		CompletableFuture<IStatus> future = new CompletableFuture<>();
		IOperationListener listener = (result) -> { future.complete(this.adapter.toRspStatus(result)); };
		getWstServer(handle.getId()).stop(force, listener);
		return future.join();
	}

	public IStatus canStop(ServerHandle handle) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId()).canStop();
		return this.adapter.toRspStatus(status);
	}

	public String getMode(ServerHandle handle) {
		return getWstServer(handle.getId()).getMode();
	}
}
