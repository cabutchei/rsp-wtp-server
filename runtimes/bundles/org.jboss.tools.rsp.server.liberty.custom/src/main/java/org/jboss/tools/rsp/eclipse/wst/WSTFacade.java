package org.jboss.tools.rsp.eclipse.wst;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IPublishListener;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.internal.Runtime;
import org.eclipse.wst.server.core.internal.RuntimeWorkingCopy;
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
import org.jboss.tools.rsp.server.spi.servertype.IServerType;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;



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
			IProject project = this.workspaceService.getProject(ref.getPath());
			IModule module = ServerUtil.getModule(project);
			// org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
			// org.eclipse.wst.server.core.IServer s;
			// s.addPublishListener(null);
			// org.eclipse.wst.server.core.ServerEvent ev;
			// IPublishListener l;
			// s.getModulePublishState(null);
			// if (modules == null || modules.length == 0) {
			// 	IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
			// 	return status;
			// }
			org.eclipse.wst.server.core.IServerWorkingCopy copy = this.registry.getWst(server.getId()).createWorkingCopy();
			try {
				// copy.modifyModules(modules, null, new NullProgressMonitor());
				copy.modifyModules(new IModule[] { module }, null, new NullProgressMonitor());
				copy.save(false, new NullProgressMonitor());
			} catch (org.eclipse.core.runtime.CoreException e) {
				e.printStackTrace();
				IStatus status = this.adapter.toRspStatus(e.getStatus());
				return status;
			}
			return Status.OK_STATUS;
	}

	public IStatus canAddDeployable(DeployableReference ref, ServerHandle server) {
		org.eclipse.core.runtime.IPath projectDescription = new org.eclipse.core.runtime.Path(ref.getPath()).append(".project");
		try {
			IProjectDescription desc = ResourcesPlugin.getWorkspace().loadProjectDescription(projectDescription);
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(desc.getName());
			// temporary hack to get around missing project in workspace
			IProjectDescription webDesc = ResourcesPlugin.getWorkspace().loadProjectDescription(new org.eclipse.core.runtime.Path("/Users/cabutchei/Documents/eclipse/runtime-run-eclipse-window/SomeWebModule").append(".project"));
			IProject web = ResourcesPlugin.getWorkspace().getRoot().getProject(webDesc.getName());
			if (!web.exists()) web.create(webDesc, new NullProgressMonitor());
			web.open(new NullProgressMonitor());
			// end
			if (!project.exists()) project.create(desc, new NullProgressMonitor());
			project.open(new NullProgressMonitor());

			//
			IVirtualComponent earVC = ComponentCore.createComponent(project);
			IVirtualComponent webVC = ComponentCore.createComponent(web);
			IVirtualReference reference = ComponentCore.createReference(earVC, webVC);
			reference.setArchiveName(webVC.getName() + ".war");
			reference.setRuntimePath(new Path("/"));
			// earVC.addReferences(new IVirtualReference[] {reference});
			//

			// TODO: jst.web is not getting picked up - need to figure out why
			IProjectFacetVersion version = ProjectFacetsManager.getProjectFacet("jst.ear").getLatestVersion();
			IProjectFacetVersion webVersion = ProjectFacetsManager.getProjectFacet("jst.web").getLatestVersion();
			Set<IProjectFacetVersion> earFacets = ProjectFacetsManager.create(project).getProjectFacets();
			Set<IProjectFacetVersion> webFacets = ProjectFacetsManager.create(web).getProjectFacets();
			org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
			if (modules == null || modules.length == 0) {
				IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
				return status;
			}
			org.eclipse.core.runtime.IStatus status = this.registry.getWst(server.getId()).canModifyModules(modules, null, new NullProgressMonitor());
			// this.registry.getWst(server.getId()).canModifyModules(modules, null, new NullProgressMonitor())
			org.eclipse.wst.server.core.IServerWorkingCopy copy = this.registry.getWst(server.getId()).createWorkingCopy();
			// copy.modifyModules(modules, null, new NullProgressMonitor());
			// copy.save(false, new NullProgressMonitor());
			return this.adapter.toRspStatus(status);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			IStatus status = this.adapter.toRspStatus(e.getStatus());
			return status;
		}
	}

	public IStatus removeDeployable(DeployableReference ref, ServerHandle server) {
		org.eclipse.core.runtime.IPath projectDescription = new org.eclipse.core.runtime.Path(ref.getPath()).append(".project");
		try {
			IProjectDescription desc = ResourcesPlugin.getWorkspace().loadProjectDescription(projectDescription);
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(desc.getName());
			if (!project.exists()) project.create(desc, new NullProgressMonitor());
			project.open(new NullProgressMonitor());
			org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
			if (modules == null || modules.length == 0) {
				IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
				return status;
			}
			org.eclipse.wst.server.core.IServerWorkingCopy copy = this.registry.getWst(server.getId()).createWorkingCopy();
			try {
				copy.modifyModules(null, modules, new NullProgressMonitor());
				copy.save(false, new NullProgressMonitor());
			} catch (org.eclipse.core.runtime.CoreException e) {
				e.printStackTrace();
				IStatus status = this.adapter.toRspStatus(e.getStatus());
				return status;
			}
			return Status.OK_STATUS;
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			IStatus status = this.adapter.toRspStatus(e.getStatus());
			return status;
		}
	}

	public IStatus canRemoveDeployable(DeployableReference ref, ServerHandle server) {
			org.eclipse.core.runtime.IPath projectDescription = new org.eclipse.core.runtime.Path(ref.getPath()).append(".project");
			try {
				IProjectDescription desc = ResourcesPlugin.getWorkspace().loadProjectDescription(projectDescription);
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(desc.getName());
				if (!project.exists()) project.create(desc, new NullProgressMonitor());
				org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
				if (modules == null || modules.length == 0) {
					IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
					return status;
				}
				org.eclipse.core.runtime.IStatus status = this.registry.getWst(server.getId()).canModifyModules(null, modules, new NullProgressMonitor());
				return this.adapter.toRspStatus(status);
			} catch (org.eclipse.core.runtime.CoreException e) {
				e.printStackTrace();
				IStatus status = this.adapter.toRspStatus(e.getStatus());
				return status;
			}
	}

	public IStatus canPublish(ServerHandle server) {
		return this.adapter.toRspStatus(this.registry.getWst(server.getId()).canPublish());
	}

	public IStatus publish(ServerHandle handle, int rspKind) {
		// IServerListener
		// getWstServer(handle.getId()).addServerListener(
		// 	(ServerEvent event) -> {
		// 		// handle publish events here
		// 		if (event.getKind() == (ServerEvent.MODULE_CHANGE | ServerEvent.PUBLISH_STATE_CHANGE)) {
		// 			// publish started
		// 			}
		// 		}
		// );
		org.eclipse.core.runtime.IStatus status = this.registry.getWst(handle.getId())
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
		org.eclipse.wst.server.core.IModule[] modules = this.registry.getWst(server.getId()).getModules();
		return modules;
	}

	private int getModulePublishState(ServerHandle server, DeployableReference ref) {
		org.eclipse.wst.server.core.IServer wstServer = this.registry.getWst(server.getId());
		IModule module = getModule(server, ref.getPath());
		return wstServer.getModulePublishState(new IModule[] {module});
	}

	private int getModuleRunState(ServerHandle server, DeployableReference ref) {
		org.eclipse.wst.server.core.IServer wstServer = this.registry.getWst(server.getId());
		IModule module = getModule(server, ref.getPath());
		return wstServer.getModuleState(new IModule[] {module});
	}

	// public DeployableState[] getDeployableStates(ServerHandle server) {
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
		// return states.toArray(new DeployableState[0]);
		return states;
	}

	public DeployableState getDeployableState(ServerHandle handle, DeployableReference ref) {
		int runState = this.adapter.toRspServerState(getModuleRunState(handle, ref));
		int publishState = this.adapter.toRspPublishState(getModulePublishState(handle, ref));
		DeployableState ds = new DeployableState(handle, ref, runState, publishState);	
		return ds;
	}


	public void createServer(IServerType serverType, String id, Map<String, Object> attributes) throws CoreException{
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
			((RuntimeWorkingCopy) runtimeWC).setAttribute("vm-install-id", "/Users/cabutchei/.sdkman/candidates/java/21.0.2-open");
			((RuntimeWorkingCopy) runtimeWC).setAttribute("vm-install-type-id", "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");
			org.eclipse.wst.server.core.IRuntime run;
			run = runtimeWC.save(true, monitor);
			// set same id so we can retrieve the rsp-wst pairs
			org.eclipse.wst.server.core.IServerWorkingCopy server = wstServerType.createServer(id, null, run, monitor);
			// TODO: replace hardcoded attributes
			server.setHost("localhost");
			server.setAttribute("serverName",   "AppSrv01");
			wstServer = server.save(false, monitor);
			registry.register(wstServer, id);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			throw new CoreException(this.adapter.toRspStatus(e.getStatus()));
		}
	}

	public IServer getRspServer(String id) {
		return this.registry.getRsp(id);
	}

	public org.eclipse.wst.server.core.IServer getWstServer(String id) {
		return this.registry.getWst(id);
	}

	public Map<String, IServer> getServers() {
		return this.registry.getAllRspServers();
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

	public IStatus canStop(ServerHandle handle) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId()).canStop();
		return this.adapter.toRspStatus(status);
	}
}
