package com.github.cabutchei.rsp.eclipse.wst;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.IModuleHandler;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.jst.j2ee.internal.componentcore.JavaEEModuleHandler;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.UpdateServerJob;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import com.github.cabutchei.rsp.api.DefaultServerAttributes;
import com.github.cabutchei.rsp.api.dao.DeployableReference;
import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.api.dao.ModuleReference;
import com.github.cabutchei.rsp.api.dao.ModuleState;
import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.IServerListener;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;
import com.github.cabutchei.rsp.server.spi.workspace.DeployableArtifact;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import com.github.cabutchei.rsp.server.spi.workspace.DeploymentAssemblyEntry;

// import com.ibm.ws.st.core.internal.WebSphereRuntime;
// import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
// import com.ibm.ws.ast.st.v85.core.internal.WASServer;



public class WSTFacade {

	private static final String KIND_FOLDER = "folder";
	private static final String KIND_PROJECT = "project";
		private static final String KIND_ARCHIVE = "archive";

		private final ServerHandleRegistry registry;
		private final IWorkspaceService workspaceService;

		public WSTFacade(ServerHandleRegistry registry, IWorkspaceService workspaceService) {
			this.registry = Objects.requireNonNull(registry, "registry");
			this.workspaceService = workspaceService;
		}

	public ServerHandleRegistry getRegistry() {
		return registry;
	}

		public List<DeploymentAssemblyEntry> getDeploymentAssembly(IProject project) {
		if (project == null || !project.exists()) {
			return null;
		}
		List<DeploymentAssemblyEntry> entries = new ArrayList<>();
		addComponentResourceMappings(project, entries);
		addComponentReferences(project, entries);
		return Collections.unmodifiableList(entries);
	}

	public List<DeployableArtifact> listDeployableResources(ServerHandle server) {
		if (workspaceService == null || server == null) {
			return Collections.emptyList();
		}
		org.eclipse.wst.server.core.IServer wstServer = getWstServer(server.getId());
		if (wstServer == null || wstServer.getServerType() == null || wstServer.getServerType().getRuntimeType() == null) {
			return Collections.emptyList();
		}
		IModule[] deployedModules = wstServer.getModules();
		Set<IModule> deployed = deployedModules == null
				? Collections.emptySet()
				: new HashSet<>(Arrays.asList(deployedModules));
		IModuleType[] moduleTypes = wstServer.getServerType().getRuntimeType().getModuleTypes();
		IModule[] modules = moduleTypes == null ? new IModule[0] : ServerUtil.getModules(moduleTypes);
		List<DeployableArtifact> results = new ArrayList<>();
		Set<String> seenProjects = new HashSet<>();
		if (modules != null) {
			for (IModule module : modules) {
				if (module == null || deployed.contains(module)) {
					continue;
				}
				IModule[] parents;
				try {
					parents = wstServer.getRootModules(module, null);
				} catch (org.eclipse.core.runtime.CoreException ce) {
					continue;
				}
				if (parents == null || parents.length == 0) {
					continue;
				}
				boolean isRoot = false;
				for (IModule parent : parents) {
					if (module.equals(parent)) {
						isRoot = true;
						break;
					}
				}
				if (!isRoot) {
					continue;
				}
				org.eclipse.core.runtime.IStatus status = wstServer.canModifyModules(new IModule[] { module }, null, null);
				if (status != null && !status.isOK()) {
					continue;
				}
				IProject project = module.getProject();
				if (project == null || !seenProjects.add(project.getName())) {
					continue;
				}
				IPath location = project.getLocation();
				java.nio.file.Path deployPath = location == null ? null : location.toFile().toPath();
				String typeId = module.getModuleType() == null ? null : module.getModuleType().getId();
				results.add(new DeployableArtifact(project.getName(),
						ServerUtil.getModuleDisplayName(module),
						deployPath,
						typeId));
			}
		}
		return results;
	}

	public List<IProject> listDeploymentAssemblyProjects(java.nio.file.Path projectPath, String projectName) {
		IProject project = resolveProject(projectPath, projectName);
		if (project == null || !project.exists()) {
			return Collections.emptyList();
		}
		try {
			if (!project.isOpen()) {
				project.open(new NullProgressMonitor());
			}
		} catch (org.eclipse.core.runtime.CoreException ce) {
			return Collections.emptyList();
		}
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return Collections.emptyList();
		}
		IVirtualReference[] refs = rootComponent.getReferences();
		ArrayList<IVirtualReference> currentRefs = refs == null
				? new ArrayList<>()
				: new ArrayList<>(Arrays.asList(refs));
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<IProject> availableList = getAvailableProjects(projects, currentRefs);
		IModuleHandler handler = resolveModuleHandler(rootComponent);
		List<IProject> filtered = handler.getFilteredProjectListForAdd(rootComponent, availableList);
		return filtered == null ? Collections.emptyList() : filtered;
	}

	public IStatus addDeploymentAssemblyEntry(IProject project, DeploymentAssemblyEntry entry) {
		if (project == null || !project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Project not found");
		}
		if (entry == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Entry is required");
		}
		if (isResourceMapping(entry)) {
			return addResourceMapping(project, entry);
		}
		return addReference(project, entry);
	}

	public IStatus removeDeploymentAssemblyEntry(IProject project, DeploymentAssemblyEntry entry) {
		if (project == null || !project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Project not found");
		}
		if (entry == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Entry is required");
		}
		if (isResourceMapping(entry)) {
			return removeResourceMapping(project, entry);
		}
		return removeReference(project, entry);
	}

	private IServer createServerProxy(org.eclipse.wst.server.core.IServer wstServer, IServerManagementModel managementModel) {
		return createServerProxy(wstServer, managementModel, null);
	}
	
	private IServerWorkingCopy createServerWorkingCopyProxy(org.eclipse.wst.server.core.IServerWorkingCopy wstServer, IServerManagementModel managementModel) {
		return createServerWorkingCopyProxy(wstServer, managementModel, null);
	}

	private IServerWorkingCopy createServerWorkingCopyProxy(org.eclipse.wst.server.core.IServerWorkingCopy wstServerWorkingCopy,
			IServerManagementModel managementModel, IServerDelegate delegate) {
		Objects.requireNonNull(wstServerWorkingCopy, "wstServerWorkingCopy");
		WstServerWorkingCopyProxy proxy = new WstServerWorkingCopyProxy(
			wstServerWorkingCopy, managementModel);
		return proxy;
	}
	public IServer createServerProxy(org.eclipse.wst.server.core.IServer wstServer,
			IServerManagementModel managementModel, IServerDelegate delegate) {
		Objects.requireNonNull(wstServer, "wstServer");
		WstServerProxy proxy = new WstServerProxy(
			wstServer, managementModel);
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

	private IModule[] getRootModules(IProject project, ServerHandle server) throws org.eclipse.core.runtime.CoreException {
		IModule[] candidates = ServerUtil.getModules(project);
		Set<IModule> roots = new LinkedHashSet<>();
		for (IModule m : candidates) {
			IModule[] allRoots = getWstServer(server.getId()).getRootModules(m, null);
    		for (IModule r : allRoots) {
        		roots.add(r);
    		}
		}
		return roots.toArray(new IModule[roots.size()]);
	}

	public IStatus addDeployable(DeployableReference ref, ServerHandle server) {
		IProject project = this.workspaceService.getProject(ref.getLabel());
		IModule[] modules = ServerUtil.getModules(project);
		try {
			modules = getRootModules(project, server);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			return WstModelAdapter.toRspStatus(e.getStatus());
		}
		org.eclipse.wst.server.core.IServerWorkingCopy serverWc = getWstServer(server.getId()).createWorkingCopy();
		try {
			serverWc.modifyModules(modules, null, new NullProgressMonitor());
			serverWc.save(false, new NullProgressMonitor());
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			IStatus status = WstModelAdapter.toRspStatus(e.getStatus());
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
		try {
			modules = getRootModules(project, server);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			return WstModelAdapter.toRspStatus(e.getStatus());
		}
		org.eclipse.core.runtime.IStatus status = getWstServer(server.getId()).canModifyModules(modules, null, new NullProgressMonitor());
		return WstModelAdapter.toRspStatus(status);
	}

	public IStatus removeDeployable(DeployableReference ref, ServerHandle server) {
		IProject project = this.workspaceService.getProject(ref.getLabel());
		if (!project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, NLS.bind("{0} isn't bound to any workspace project", ref.getLabel()));
		}
		IModule[] modules = ServerUtil.getModules(project);
		try {
			modules = getRootModules(project, server);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			return WstModelAdapter.toRspStatus(e.getStatus());
		}
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
			IStatus status = WstModelAdapter.toRspStatus(e.getStatus());
			return status;
		}
	}

	public IStatus canRemoveDeployable(DeployableReference ref, ServerHandle server) {
		IProject project = this.workspaceService.getProject(ref.getLabel());
		if (!project.exists()) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, NLS.bind("{0} isn't bound to any workspace project", ref.getLabel()));
		}
		org.eclipse.wst.server.core.IModule[] modules = ServerUtil.getModules(project);
		try {
			modules = getRootModules(project, server);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			return WstModelAdapter.toRspStatus(e.getStatus());
		}
		if (modules == null || modules.length == 0) {
			IStatus status = new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "No modules found in project: " + project.getName());
			return status;
		}
		org.eclipse.core.runtime.IStatus status = getWstServer(server.getId()).canModifyModules(null, modules, new NullProgressMonitor());
			return WstModelAdapter.toRspStatus(status);
	}

	public IStatus canPublish(ServerHandle server) {
		return WstModelAdapter.toRspStatus(getWstServer(server.getId()).canPublish());
	}

	public IStatus publish(ServerHandle handle, int rspKind) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId())
		.publish(WstModelAdapter.toWstPublishKind(rspKind), new NullProgressMonitor());
		return WstModelAdapter.toRspStatus(status);
	}

	public int getServerPublishState(ServerHandle handle) {
		int publishState = getWstServer(handle.getId()).getServerPublishState();
		return WstModelAdapter.toRspPublishState(publishState);
	}

	public int getServerRunState(ServerHandle handle) {
		int runState = getWstServer(handle.getId()).getServerState();
		return WstModelAdapter.toRspServerState(runState);
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

	private int getModulePublishState(org.eclipse.wst.server.core.IServer wstServer, IModule module) {
		return wstServer.getModulePublishState(new IModule[] {module});
	}

	private int getModuleRunState(org.eclipse.wst.server.core.IServer wstServer, IModule module) {
		return wstServer.getModuleState(new IModule[] {module});
	}

	public void startModule(ServerHandle server, DeployableReference ref) {
		org.eclipse.wst.server.core.IServer wstServer = getWstServer(server.getId());
		IModule module = this.getModule(server, ref.getLabel());
		this.startModule(wstServer, module);
	}

	public void stopModule(ServerHandle server, DeployableReference ref) {
		org.eclipse.wst.server.core.IServer wstServer = getWstServer(server.getId());
		IModule module = this.getModule(server, ref.getLabel());
		this.stopModule(wstServer, module);
	}

	private void startModule(org.eclipse.wst.server.core.IServer wstServer, IModule module) {
		wstServer.startModule(new IModule[] { module }, null);
	}

	private void stopModule(org.eclipse.wst.server.core.IServer wstServer, IModule module) {
		wstServer.stopModule(new IModule[] { module }, null);
	}

	private DeployableReference toDeployableReference(IModule module) {
		String label = module.getName();
		String path = module.getProject() != null ? module.getProject().getLocation().toOSString() : module.getName();
		return new DeployableReference(label, path, module.getModuleType().getId());
	}

	private ModuleReference toModuleReference(IModule module) {
		String typeId = module.getModuleType() != null ? module.getModuleType().getId() : null;
		return new ModuleReference(module.getId(), module.getName(), typeId);
	}

	private List<IModule> collectChildModules(org.eclipse.wst.server.core.IServer wstServer, IModule module) {
		List<IModule> result = new ArrayList<>();
		IModule[] children = wstServer.getChildModules(new IModule[] {module}, new NullProgressMonitor());
		if (children != null) {
			for (IModule child : children) {
				result.add(child);
				result.addAll(collectChildModules(wstServer, child));
			}
		}
		return result;
	}

		public List<DeployableState> getDeployableStates(ServerHandle server) {
			List<DeployableState> states = new ArrayList<>();
			IModule[] modules = getModules(server);
			for (IModule module : modules) {
				DeployableReference ref = toDeployableReference(module);
				int runState = WstModelAdapter.toRspServerState(getModuleRunState(server, ref));
				int publishState = WstModelAdapter.toRspPublishState(getModulePublishState(server, ref));
				DeployableState ds = new DeployableState(server, ref, runState, publishState);
				states.add(ds);
			}
			return states;
		}

	public List<ModuleState> getModuleStates(ServerHandle server) {
		List<ModuleState> states = new ArrayList<>();
		org.eclipse.wst.server.core.IServer wstServer = getWstServer(server.getId());
		IModule[] modules = getModules(server);
		if (modules == null) {
			return states;
		}
		for (IModule module : modules) {
			DeployableReference deployable = toDeployableReference(module);
			List<IModule> children = collectChildModules(wstServer, module);
				for (IModule child : children) {
					ModuleReference moduleRef = toModuleReference(child);
					int runState = WstModelAdapter.toRspServerState(getModuleRunState(wstServer, child));
					int publishState = WstModelAdapter.toRspPublishState(getModulePublishState(wstServer, child));
					states.add(new ModuleState(deployable, moduleRef, runState, publishState));
				}
			}
			return states;
		}

		public DeployableState getDeployableState(ServerHandle handle, DeployableReference ref) {
			int runState = WstModelAdapter.toRspServerState(getModuleRunState(handle, ref));
			int publishState = WstModelAdapter.toRspPublishState(getModulePublishState(handle, ref));
			DeployableState ds = new DeployableState(handle, ref, runState, publishState);	
			return ds;
		}


	public IServerWorkingCopy createServer(IServerType serverType, String id, Map<String, Object> attributes, IServerManagementModel model) throws CoreException {
		org.eclipse.wst.server.core.IServer wstServer = null;
		if (serverType == null) {
			throw new IllegalArgumentException("serverType cannot be null");
			}
			org.eclipse.core.runtime.IProgressMonitor monitor = new NullProgressMonitor();
			org.eclipse.wst.server.core.IServerType wstServerType = WstModelAdapter.toWstServerType(serverType);
			if (wstServerType == null) {
				throw new CoreException(new Status(IStatus.ERROR, "", "Server Type " + serverType.getId() + " is unknown by this Eclipse application"));
			}
		org.eclipse.wst.server.core.IRuntimeType wstRuntimeType = wstServerType.getRuntimeType();
		org.eclipse.wst.server.core.IRuntimeWorkingCopy runtimeWC;
		try {
			runtimeWC = wstRuntimeType.createRuntime((String)null, monitor);
			runtimeWC.setLocation(new org.eclipse.core.runtime.Path((String) attributes.get(DefaultServerAttributes.SERVER_HOME_DIR)));
			// TODO: let the user choose the vm?
			// runtimeWC.getAdapter(RuntimeWorkingCopy.class).setAttribute("vm-install-id", "/Users/cabutchei/.sdkman/candidates/java/21.0.2-open");
			// runtimeWC.getAdapter(RuntimeWorkingCopy.class).setAttribute("vm-install-type-id", "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");
			org.eclipse.wst.server.core.IRuntime run = runtimeWC.save(true, monitor);
			// set same id so we can retrieve the rsp-wst pairs
			org.eclipse.wst.server.core.IServerWorkingCopy server = wstServerType.createServer(id, null, run, monitor);
			server.setName(id);
			applyAttributes(server, attributes);
			WstServerTypeHandler handler = WstServerTypeHandlerRegistry.find(serverType.getId());
			if( handler != null ) {
				handler.configureServer(server, runtimeWC, attributes, monitor);
			}
			return createServerWorkingCopyProxy(server, model);
		} catch (org.eclipse.core.runtime.CoreException e) {
			e.printStackTrace();
			throw new CoreException(WstModelAdapter.toRspStatus(e.getStatus()));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void applyAttributes(org.eclipse.wst.server.core.IServerWorkingCopy server, Map<String, Object> attributes) {
		if (server == null || attributes == null || attributes.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (key == null || value == null) {
				continue;
			}
			if (value instanceof Integer) {
				server.setAttribute(key, ((Integer) value).intValue());
			} else if (value instanceof Boolean) {
				server.setAttribute(key, ((Boolean) value).booleanValue());
			} else if (value instanceof String) {
				server.setAttribute(key, (String) value);
			} else if (value instanceof List) {
				server.setAttribute(key, (List) value);
			} else if (value instanceof Map) {
				server.setAttribute(key, (Map) value);
			}
		}
	}

	public IServer getRspServer(String id) {
		return this.registry.getRsp(id);
	}

	public org.eclipse.wst.server.core.IServer getWstServer(String id) {
		return ServerCore.findServer(id);
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

	public IStatus start(ServerHandle handle, String launchMode) {
		CompletableFuture<IStatus> future = new CompletableFuture<>();
		IOperationListener listener = (result) -> { future.complete(WstModelAdapter.toRspStatus(result)); };
		IJBoss7Mana
		getWstServer(handle.getId()).start(launchMode, listener);
		return Status.OK_STATUS;
	}

	// public void start(ServerHandle handle, String launchMode) throws CoreException {
	// 	try {
	// 		getWstServer(handle.getId()).start(launchMode, (IProgressMonitor) null);
	// 	} catch (org.eclipse.core.runtime.CoreException e) {
	// 		e.printStackTrace();
	// 		throw new CoreException(this.adapter.toRspStatus(e.getStatus()));
	// 	}
	// }

	public IStatus canStart(ServerHandle handle, String launchMode) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId()).canStart(launchMode);
		return WstModelAdapter.toRspStatus(status);
	}

	public void stop(ServerHandle handle, boolean force) {
		getWstServer(handle.getId()).stop(force);
	}

	public IStatus stopSync(ServerHandle handle, boolean force) {
		CompletableFuture<IStatus> future = new CompletableFuture<>();
		IOperationListener listener = (result) -> { future.complete(WstModelAdapter.toRspStatus(result)); };
		getWstServer(handle.getId()).stop(force, listener);
		return future.join();
	}

	public IStatus canStop(ServerHandle handle) {
		org.eclipse.core.runtime.IStatus status = getWstServer(handle.getId()).canStop();
		return WstModelAdapter.toRspStatus(status);
	}

	public String getMode(ServerHandle handle) {
		return getWstServer(handle.getId()).getMode();
	}

	public void updateServerStatus() {
		org.eclipse.wst.server.core.IServer[] servers = ServerCore.getServers();
		Job job = new UpdateServerJob(servers);
		job.schedule();
	}

	private void addComponentResourceMappings(IProject project, List<DeploymentAssemblyEntry> entries) {
		StructureEdit structureEdit = null;
		try {
			structureEdit = StructureEdit.getStructureEditForRead(project);
			WorkbenchComponent component = structureEdit.getComponent();
			if (component == null) {
				return;
			}
			Object[] resources = component.getResources().toArray();
			for (Object resourceObj : resources) {
				if (!(resourceObj instanceof ComponentResource)) {
					continue;
				}
				ComponentResource resource = (ComponentResource) resourceObj;
				IPath sourcePath = resource.getSourcePath();
				IPath runtimePath = resource.getRuntimePath();
				if (sourcePath == null || runtimePath == null) {
					continue;
				}
				entries.add(new DeploymentAssemblyEntry(
						sourcePath.toString(),
						normalizeRuntimePath(runtimePath),
						KIND_FOLDER,
						KIND_FOLDER));
			}
		} catch (Exception e) {
			return;
		} catch (Throwable e) {
			return;
		} finally {
			if (structureEdit != null) {
				structureEdit.dispose();
			}
		}
	}

	private void addComponentReferences(IProject project, List<DeploymentAssemblyEntry> entries) {
		IVirtualComponent component = ComponentCore.createComponent(project);
		if (component == null) {
			return;
		}
		HashMap<String, Object> options = new HashMap<>();
		options.put(IVirtualComponent.REQUESTED_REFERENCE_TYPE, IVirtualComponent.DISPLAYABLE_REFERENCES_ALL);
		IVirtualReference[] refs = component.getReferences(options);
		if (refs == null) {
			return;
		}
		for (IVirtualReference ref : refs) {
			if (ref == null) {
				continue;
			}
			String sourceText = resolveReferenceSource(ref.getReferencedComponent());
			String runtimeText = normalizeRuntimePath(new Path(getSafeRuntimePath(ref)));
			entries.add(new DeploymentAssemblyEntry(
					sourceText,
					runtimeText,
					resolveReferenceSourceKind(ref.getReferencedComponent()),
					KIND_ARCHIVE));
		}
	}

	private String resolveReferenceSource(IVirtualComponent component) {
		if (component == null) {
			return "";
		}
		if (component.isBinary()) {
			IPath componentPath = component.getAdapter(IPath.class);
			return componentPath == null ? component.getName() : componentPath.toString();
		}
		IProject project = component.getProject();
		return project == null ? component.getName() : project.getName();
	}

	private String resolveReferenceSourceKind(IVirtualComponent component) {
		if (component != null && component.isBinary()) {
			return KIND_ARCHIVE;
		}
		return KIND_PROJECT;
	}

	private String getSafeRuntimePath(IVirtualReference ref) {
		String archiveName = ref.getDependencyType() == IVirtualReference.DEPENDENCY_TYPE_CONSUMES
				? null
				: ref.getArchiveName();
		String value;
		if (archiveName != null) {
			IPath runtimePath = new Path(archiveName);
			if (runtimePath.segmentCount() > 1) {
				value = archiveName;
			} else {
				value = ref.getRuntimePath().append(archiveName).toString();
			}
		} else {
			value = ref.getRuntimePath().toString();
		}
		return value == null ? "/" : value;
	}

	private String normalizeRuntimePath(IPath runtimePath) {
		if (runtimePath.isRoot()) {
			return runtimePath.toString();
		}
		return runtimePath.makeRelative().toString();
	}

	private boolean isResourceMapping(DeploymentAssemblyEntry entry) {
		return KIND_FOLDER.equals(entry.getSourceKind()) && KIND_FOLDER.equals(entry.getDeployKind());
	}

	private IStatus addResourceMapping(IProject project, DeploymentAssemblyEntry entry) {
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Component not available");
		}
		String deployPath = entry.getDeployPath() == null ? "/" : entry.getDeployPath();
		IPath runtimePath = new Path(deployPath).makeAbsolute();
		IPath sourcePath = new Path(entry.getSourcePath());
		IVirtualFolder rootFolder = rootComponent.getRootFolder();
		try {
			rootFolder.getFolder(runtimePath).createLink(sourcePath, 0, null);
			return Status.OK_STATUS;
		} catch (org.eclipse.core.runtime.CoreException e) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Failed to add resource mapping", e);
		}
	}

	private IStatus removeResourceMapping(IProject project, DeploymentAssemblyEntry entry) {
		StructureEdit structureEdit = null;
		try {
			structureEdit = StructureEdit.getStructureEditForWrite(project);
			WorkbenchComponent component = structureEdit.getComponent();
			if (component == null) {
				return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Component not available");
			}
			String targetRuntimePath = normalizeRuntimePath(new Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath()));
			String targetSource = entry.getSourcePath();
			List<?> resources = component.getResources();
			for (int i = resources.size() - 1; i >= 0; i--) {
				Object obj = resources.get(i);
				if (!(obj instanceof ComponentResource)) {
					continue;
				}
				ComponentResource resource = (ComponentResource) obj;
				IPath sourcePath = resource.getSourcePath();
				IPath runtimePath = resource.getRuntimePath();
				if (sourcePath == null || runtimePath == null) {
					continue;
				}
				String runtimeNormalized = normalizeRuntimePath(runtimePath);
				if (sourcePath.toString().equals(targetSource) && runtimeNormalized.equals(targetRuntimePath)) {
					resources.remove(i);
				}
			}
			return Status.OK_STATUS;
		} finally {
			if (structureEdit != null) {
				structureEdit.saveIfNecessary(new NullProgressMonitor());
				structureEdit.dispose();
			}
		}
	}

	private IStatus addReference(IProject project, DeploymentAssemblyEntry entry) {
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Component not available");
		}
		IVirtualComponent targetComponent = resolveReferencedComponent(project, entry);
		if (targetComponent == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Referenced component not found");
		}
		IPath runtimePath = new Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath()).makeAbsolute();
		IVirtualReference reference = ComponentCore.createReference(rootComponent, targetComponent, runtimePath);
		if (KIND_ARCHIVE.equals(entry.getSourceKind())) {
			String sourcePath = entry.getSourcePath();
			if (sourcePath != null && !sourcePath.isEmpty()) {
				reference.setArchiveName(new File(sourcePath).getName());
			}
		}
		rootComponent.addReferences(new IVirtualReference[] { reference });
		return Status.OK_STATUS;
	}

	private IStatus removeReference(IProject project, DeploymentAssemblyEntry entry) {
		IVirtualComponent rootComponent = ComponentCore.createComponent(project);
		if (rootComponent == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Component not available");
		}
		String targetSource = entry.getSourcePath();
		String targetDeploy = normalizeRuntimePath(new Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath()));
		IVirtualReference[] refs = rootComponent.getReferences();
		if (refs == null || refs.length == 0) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Reference not found");
		}
		ArrayList<IVirtualReference> updated = new ArrayList<>(Arrays.asList(refs));
		boolean removed = false;
		for (IVirtualReference ref : refs) {
			if (ref == null) {
				continue;
			}
			if (!matchesReferenceSource(ref, targetSource, entry.getSourceKind())) {
				continue;
			}
			String runtimeText = normalizeRuntimePath(new Path(getSafeRuntimePath(ref)));
			if (runtimeText.equals(targetDeploy)) {
				updated.remove(ref);
				removed = true;
			}
		}
		if (!removed) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, "Reference not found");
		}
		rootComponent.setReferences(updated.toArray(new IVirtualReference[0]));
		return Status.OK_STATUS;
	}

	private IVirtualComponent resolveReferencedComponent(IProject project, DeploymentAssemblyEntry entry) {
		if (KIND_PROJECT.equals(entry.getSourceKind())) {
			if (workspaceService == null) {
				return null;
			}
			IProject referenced = workspaceService.getProject(entry.getSourcePath());
			return referenced == null ? null : ComponentCore.createComponent(referenced);
		}
		if (KIND_ARCHIVE.equals(entry.getSourceKind())) {
			String sourcePath = entry.getSourcePath();
			if (sourcePath == null || sourcePath.isEmpty()) {
				return null;
			}
			String componentName = VirtualArchiveComponent.LIBARCHIVETYPE + IPath.SEPARATOR + sourcePath;
			IPath runtimePath = new Path(entry.getDeployPath() == null ? "/" : entry.getDeployPath()).makeAbsolute();
			return ComponentCore.createArchiveComponent(project, componentName, runtimePath);
		}
		return null;
	}

	private boolean matchesReferenceSource(IVirtualReference ref, String targetSource, String sourceKind) {
		IVirtualComponent component = ref.getReferencedComponent();
		if (component == null) {
			return false;
		}
		if (KIND_PROJECT.equals(sourceKind)) {
			return component.getProject() != null && component.getProject().getName().equals(targetSource);
		}
		IPath componentPath = component.getAdapter(IPath.class);
		if (componentPath != null) {
			return componentPath.toString().equals(targetSource);
		}
		return component.getName() != null && component.getName().equals(targetSource);
	}

	private ArrayList<IProject> getAvailableProjects(IProject[] projects, ArrayList<IVirtualReference> currentRefs) {
		if (projects == null || projects.length == 0) {
			return new ArrayList<>();
		}
		if (currentRefs == null || currentRefs.isEmpty()) {
			return new ArrayList<>(Arrays.asList(projects));
		}
		ArrayList<IProject> available = new ArrayList<>();
		for (IProject proj : projects) {
			if (proj == null) {
				continue;
			}
			boolean matches = false;
			for (int j = 0; j < currentRefs.size() && !matches; j++) {
				IVirtualReference ref = currentRefs.get(j);
				if (ref == null) {
					continue;
				}
				IVirtualComponent referenced = ref.getReferencedComponent();
				IProject referencedProject = referenced == null ? null : referenced.getProject();
				if (proj.equals(referencedProject) || available.contains(proj)) {
					matches = true;
				}
			}
			if (!matches) {
				available.add(proj);
			}
		}
		return available;
	}

	private IModuleHandler resolveModuleHandler(IVirtualComponent component) {
		if (component == null) {
			return new JavaEEModuleHandler();
		}
		IModuleHandler handler = component.getAdapter(IModuleHandler.class);
		return handler == null ? new JavaEEModuleHandler() : handler;
	}

	private IProject resolveProject(java.nio.file.Path projectPath, String projectName) {
		if (workspaceService != null && projectName != null && !projectName.isEmpty()) {
			IProject project = workspaceService.getProject(projectName);
			if (project != null && project.exists()) {
				return project;
			}
		}
		if (projectPath == null) {
			return null;
		}
		java.nio.file.Path normalized = projectPath.toAbsolutePath().normalize();
		IProject bestMatch = null;
		int bestSegments = -1;
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IPath location = project.getLocation();
			if (location == null) {
				continue;
			}
			java.nio.file.Path projectLocation = location.toFile().toPath().toAbsolutePath().normalize();
			if (!normalized.startsWith(projectLocation)) {
				continue;
			}
			int segments = projectLocation.getNameCount();
			if (segments > bestSegments) {
				bestMatch = project;
				bestSegments = segments;
			}
		}
		return bestMatch;
	}
}
