package com.github.cabutchei.rsp.eclipse.wst;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;
import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.DeployableReference;
import com.github.cabutchei.rsp.eclipse.core.runtime.MultiStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;



/**
 * Adapts WST/Eclipse runtime objects to the RSP runtime equivalents.
 */
public class WstModelAdapter {
	private static final String UNKNOWN_PLUGIN_ID = "unknown";

	public com.github.cabutchei.rsp.eclipse.core.runtime.IStatus toRspStatus(IStatus status) {
		if (status == null) {
			return new Status(Status.ERROR, UNKNOWN_PLUGIN_ID, "WST status was null");
		}
		String pluginId = normalizePluginId(status.getPlugin());
		if (status.isMultiStatus()) {
			com.github.cabutchei.rsp.eclipse.core.runtime.IStatus[] children = convertChildren(status.getChildren());
			return new MultiStatus(pluginId, status.getCode(), children, status.getMessage(), status.getException());
		}
		return new Status(status.getSeverity(), pluginId, status.getCode(), status.getMessage(), status.getException());
	}

	public int toRspPublishKind(int wstPublishKind) {
		switch (wstPublishKind) {
			case IServer.PUBLISH_INCREMENTAL:
				return ServerManagementAPIConstants.PUBLISH_INCREMENTAL;
			case IServer.PUBLISH_FULL:
				return ServerManagementAPIConstants.PUBLISH_FULL;
			case IServer.PUBLISH_AUTO:
				return ServerManagementAPIConstants.PUBLISH_AUTO;
			case IServer.PUBLISH_CLEAN:
				return ServerManagementAPIConstants.PUBLISH_CLEAN;
			default:
				return wstPublishKind;
		}
	}

	public int toWstPublishKind(int rspPublishKind) {
		switch (rspPublishKind) {
			case ServerManagementAPIConstants.PUBLISH_INCREMENTAL:
				return IServer.PUBLISH_INCREMENTAL;
			case ServerManagementAPIConstants.PUBLISH_FULL:
				return IServer.PUBLISH_FULL;
			case ServerManagementAPIConstants.PUBLISH_AUTO:
				return IServer.PUBLISH_AUTO;
			case ServerManagementAPIConstants.PUBLISH_CLEAN:
				return IServer.PUBLISH_CLEAN;
			default:
				return rspPublishKind;
		}
	}

	public int toRspPublishState(int wstPublishState) {
		switch (wstPublishState) {
			case IServer.PUBLISH_STATE_NONE:
				return ServerManagementAPIConstants.PUBLISH_STATE_NONE;
			case IServer.PUBLISH_STATE_INCREMENTAL:
				return ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL;
			case IServer.PUBLISH_STATE_FULL:
				return ServerManagementAPIConstants.PUBLISH_STATE_FULL;
			// case IServer.PUBLISH_STATE_ADD:
			// 	return ServerManagementAPIConstants.PUBLISH_STATE_ADD;
			// case IServer.PUBLISH_STATE_REMOVE:
			// 	return ServerManagementAPIConstants.PUBLISH_STATE_REMOVE;
			case IServer.PUBLISH_STATE_UNKNOWN:
				return ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN;
			default:
				return ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN;
		}
	}

	public int toWstPublishState(int rspPublishState) {
		switch (rspPublishState) {
			case ServerManagementAPIConstants.PUBLISH_STATE_NONE:
				return IServer.PUBLISH_STATE_NONE;
			case ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL:
				return IServer.PUBLISH_STATE_INCREMENTAL;
			case ServerManagementAPIConstants.PUBLISH_STATE_FULL:
				return IServer.PUBLISH_STATE_FULL;
			// case ServerManagementAPIConstants.PUBLISH_STATE_ADD:
			// 	return IServer.PUBLISH_STATE_ADD;
			// case ServerManagementAPIConstants.PUBLISH_STATE_REMOVE:
			// 	return IServer.PUBLISH_STATE_REMOVE;
			case ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN:
				return IServer.PUBLISH_STATE_UNKNOWN;
			default:
				return IServer.PUBLISH_STATE_UNKNOWN;
		}
	}

	public int toRspServerState(int wstServerState) {
		switch (wstServerState) {
			case IServer.STATE_STARTING:
				return ServerManagementAPIConstants.STATE_STARTING;
			case IServer.STATE_STARTED:
				return ServerManagementAPIConstants.STATE_STARTED;
			case IServer.STATE_STOPPING:
				return ServerManagementAPIConstants.STATE_STOPPING;
			case IServer.STATE_STOPPED:
				return ServerManagementAPIConstants.STATE_STOPPED;
			case IServer.STATE_UNKNOWN:
				return ServerManagementAPIConstants.STATE_UNKNOWN;
			default:
				return ServerManagementAPIConstants.STATE_UNKNOWN;
		}
	}

	public int toWstServerState(int rspServerState) {
		switch (rspServerState) {
			case ServerManagementAPIConstants.STATE_STARTING:
				return IServer.STATE_STARTING;
			case ServerManagementAPIConstants.STATE_STARTED:
				return IServer.STATE_STARTED;
			case ServerManagementAPIConstants.STATE_STOPPING:
				return IServer.STATE_STOPPING;
			case ServerManagementAPIConstants.STATE_STOPPED:
				return IServer.STATE_STOPPED;
			case ServerManagementAPIConstants.STATE_UNKNOWN:
				return IServer.STATE_UNKNOWN;
			default:
				return IServer.STATE_UNKNOWN;
		}
	}

	public com.github.cabutchei.rsp.server.spi.servertype.IServerType toRspServerType(IServerType wstType, IServerModel model) {
		if (wstType == null || model == null) {
			return null;
		}
		return model.getIServerType(wstType.getId());
	}

	public com.github.cabutchei.rsp.server.spi.servertype.IServerType toRspServerType(String typeId, IServerModel model) {
		if (typeId == null || model == null) {
			return null;
		}
		return model.getIServerType(typeId);
	}

	public IServerType toWstServerType(com.github.cabutchei.rsp.server.spi.servertype.IServerType rspType) {
		if (rspType == null) {
			return null;
		}
		return toWstServerType(rspType.getId());
	}

	public IServerType toWstServerType(String typeId) {
		if (typeId == null) {
			return null;
		}
		for (IServerType serverType : ServerCore.getServerTypes()) {
			if (typeId.equals(serverType.getId())) {
				return serverType;
			}
		}
		return null;
	}

	public com.github.cabutchei.rsp.server.spi.servertype.ServerEvent toRspServerEvent(ServerEvent event,
			com.github.cabutchei.rsp.server.spi.servertype.IServer rspServer) {
		if (event == null || rspServer == null) {
			return null;
		}
		DeployableReference[] deployables = toDeployableReferences(event.getModule());
		int state = toRspServerState(event.getState());
		int publishState = toRspPublishState(event.getPublishState());
		com.github.cabutchei.rsp.eclipse.core.runtime.IStatus status = toRspStatusOrNull(event.getStatus());
		return new com.github.cabutchei.rsp.server.spi.servertype.ServerEvent(event.getKind(), rspServer, deployables,
				state, publishState, event.getRestartState(), status);
	}

	public ServerEvent toWstServerEvent(com.github.cabutchei.rsp.server.spi.servertype.ServerEvent event,
			org.eclipse.wst.server.core.IServer wstServer) {
		if (event == null || wstServer == null) {
			return null;
		}
		IModule[] modules = toWstModules(event.getDeployables(), wstServer);
		int state = toWstServerState(event.getState());
		int publishState = toWstPublishState(event.getPublishState());
		boolean restartState = event.getRestartState();
		org.eclipse.core.runtime.IStatus status = toWstStatus(event.getStatus());
		int kind = event.getKind();
		if (modules != null && modules.length > 0) {
			kind = kind & ~ServerEvent.SERVER_CHANGE;
			if (status != null) {
				return new ServerEvent(kind, wstServer, modules, state, publishState, restartState, status);
			}
			return new ServerEvent(kind, wstServer, modules, state, publishState, restartState);
		}
		kind = kind & ~ServerEvent.MODULE_CHANGE;
		if (status != null) {
			return new ServerEvent(kind, wstServer, state, publishState, restartState, status);
		}
		return new ServerEvent(kind, wstServer, state, publishState, restartState);
	}

	private DeployableReference[] toDeployableReferences(IModule[] modules) {
		if (modules == null || modules.length == 0) {
			return null;
		}
		List<DeployableReference> refs = new ArrayList<>(modules.length);
		for (IModule module : modules) {
			if (module == null) {
				continue;
			}
			String label = module.getName();
			String path = label;
			if (module.getProject() != null && module.getProject().getName() != null) {
				path = module.getProject().getName();
			}
			refs.add(new DeployableReference(label, path));
		}
		if (refs.isEmpty()) {
			return null;
		}
		return refs.toArray(new DeployableReference[0]);
	}

	private IModule[] toWstModules(DeployableReference[] deployables, org.eclipse.wst.server.core.IServer wstServer) {
		if (deployables == null || deployables.length == 0 || wstServer == null) {
			return null;
		}
		IModule[] available = wstServer.getModules();
		if (available == null || available.length == 0) {
			return null;
		}
		List<IModule> matches = new ArrayList<>();
		for (DeployableReference deployable : deployables) {
			for (IModule module : available) {
				if (matchesModule(deployable, module)) {
					matches.add(module);
					break;
				}
			}
		}
		if (matches.isEmpty()) {
			return null;
		}
		return matches.toArray(new IModule[0]);
	}

	private boolean matchesModule(DeployableReference deployable, IModule module) {
		if (deployable == null || module == null) {
			return false;
		}
		String label = deployable.getLabel();
		String path = deployable.getPath();
		if (label != null && label.equals(module.getName())) {
			return true;
		}
		if (module.getProject() != null && module.getProject().getName() != null) {
			String projectName = module.getProject().getName();
			if (label != null && label.equals(projectName)) {
				return true;
			}
			if (path != null && path.equals(projectName)) {
				return true;
			}
		}
		return path != null && path.equals(module.getName());
	}

	private com.github.cabutchei.rsp.eclipse.core.runtime.IStatus toRspStatusOrNull(IStatus status) {
		if (status == null) {
			return null;
		}
		return toRspStatus(status);
	}

	private org.eclipse.core.runtime.IStatus toWstStatus(com.github.cabutchei.rsp.eclipse.core.runtime.IStatus status) {
		if (status == null) {
			return null;
		}
		String pluginId = normalizePluginId(status.getPlugin());
		if (status.isMultiStatus()) {
			org.eclipse.core.runtime.IStatus[] children = toWstChildren(status.getChildren());
			return new org.eclipse.core.runtime.MultiStatus(pluginId, status.getCode(), children, status.getMessage(),
					status.getException());
		}
		return new org.eclipse.core.runtime.Status(status.getSeverity(), pluginId, status.getCode(), status.getMessage(),
				status.getException());
	}

	private org.eclipse.core.runtime.IStatus[] toWstChildren(com.github.cabutchei.rsp.eclipse.core.runtime.IStatus[] children) {
		if (children == null || children.length == 0) {
			return new org.eclipse.core.runtime.IStatus[0];
		}
		org.eclipse.core.runtime.IStatus[] converted = new org.eclipse.core.runtime.IStatus[children.length];
		for (int i = 0; i < children.length; i++) {
			converted[i] = toWstStatus(children[i]);
		}
		return converted;
	}

	private com.github.cabutchei.rsp.eclipse.core.runtime.IStatus[] convertChildren(IStatus[] children) {
		if (children == null || children.length == 0) {
			return new com.github.cabutchei.rsp.eclipse.core.runtime.IStatus[0];
		}
		com.github.cabutchei.rsp.eclipse.core.runtime.IStatus[] converted = new com.github.cabutchei.rsp.eclipse.core.runtime.IStatus[children.length];
		for (int i = 0; i < children.length; i++) {
			converted[i] = toRspStatus(children[i]);
		}
		return converted;
	}

	private String normalizePluginId(String pluginId) {
		if (pluginId == null || pluginId.isEmpty()) {
			return UNKNOWN_PLUGIN_ID;
		}
		return pluginId;
	}
}
