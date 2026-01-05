/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import java.util.Objects;

import org.jboss.tools.rsp.server.spi.model.DefaultServerLifecycleStrategy;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;
import org.jboss.tools.rsp.eclipse.workspace.EclipseWorkspaceService;

/**
 * Owns WST integration components and installs the lifecycle strategy.
 */
public final class WstIntegrationService implements IWstIntegrationService {

	private final ServerHandleRegistry registry;
	private final WSTServerFacade facade;
	private final WstServerLifecycleStrategy lifecycleStrategy;
	private final WstModelAdapter adapter;
	private final IWorkspaceService workspaceService;

	public WstIntegrationService() {
		this(new ServerHandleRegistry());
	}

	public WstIntegrationService(ServerHandleRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.adapter = new WstModelAdapter();
		this.facade = new WSTServerFacade(this.registry, this.adapter);
		this.lifecycleStrategy = new WstServerLifecycleStrategy(this.facade);
		this.workspaceService = new EclipseWorkspaceService();
	}

	public void initialize(IServerManagementModel managementModel) {
		refreshServers(managementModel);
	}

	@Override
	public WSTServerFacade getFacade() {
		return facade;
	}

	@Override
	public ServerHandleRegistry getRegistry() {
		return registry;
	}

	@Override
	public WstModelAdapter getAdapter() {
		return adapter;
	}

	@Override
	public IWorkspaceService getWorkspaceService() {
		return workspaceService;
	}

	@Override
	public WstServerLifecycleStrategy getLifecycleStrategy() {
		return lifecycleStrategy;
	}

	@Override
	public void install(IServerModel model) {
		if (model != null) {
			model.setServerLifecycleStrategy(lifecycleStrategy);
		}
	}

	@Override
	public void uninstall(IServerModel model) {
		if (model != null) {
			model.setServerLifecycleStrategy(DefaultServerLifecycleStrategy.INSTANCE);
		}
	}

	@Override
	public void dispose(IServerModel model) {
		uninstall(model);
		facade.dispose();
	}

	@Override
	public void refreshServers(IServerManagementModel managementModel) {
		IServer[] servers = this.facade.createServeProxies(managementModel);
		for (IServer server : servers) {
			this.registry.register(server);
		}
	}
}
