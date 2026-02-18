package com.github.cabutchei.rsp.eclipse.wst.core;

import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.wst.api.IWstIntegrationService;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;

import com.github.cabutchei.rsp.eclipse.wst.workspace.EclipseWorkspaceService;

/**
 * Owns WST integration components
 */
public final class WstIntegrationService implements IWstIntegrationService {

	private final ServerHandleRegistry registry;
	private final WSTFacade facade;
	private final IWorkspaceService workspaceService;

	public WstIntegrationService() {
		this(new ServerHandleRegistry());
	}

	public WstIntegrationService(ServerHandleRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.workspaceService = new EclipseWorkspaceService();
		this.facade = new WSTFacade(this.registry, this.workspaceService);
		if (this.workspaceService instanceof EclipseWorkspaceService) {
			((EclipseWorkspaceService) this.workspaceService).setWstFacade(this.facade);
		}
	}

	@Override
	public WSTFacade getFacade() {
		return facade;
	}

	@Override
	public ServerHandleRegistry getRegistry() {
		return registry;
	}

	@Override
	public IWorkspaceService getWorkspaceService() {
		return workspaceService;
	}

	@Override
	public void dispose(IServerModel model) {
		facade.dispose();
	}
}
