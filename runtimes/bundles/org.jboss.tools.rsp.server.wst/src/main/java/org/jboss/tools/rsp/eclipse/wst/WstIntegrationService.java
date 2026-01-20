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

import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;
import org.jboss.tools.rsp.eclipse.workspace.EclipseWorkspaceService;

/**
 * Owns WST integration components
 */
public final class WstIntegrationService implements IWstIntegrationService {

	private final ServerHandleRegistry registry;
	private final WSTFacade facade;
	private final WstModelAdapter adapter;
	private final IWorkspaceService workspaceService;

	public WstIntegrationService() {
		this(new ServerHandleRegistry());
	}

	public WstIntegrationService(ServerHandleRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.adapter = new WstModelAdapter();
		this.workspaceService = new EclipseWorkspaceService();
		this.facade = new WSTFacade(this.registry, this.adapter, this.workspaceService);
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
	public WstModelAdapter getAdapter() {
		return adapter;
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
