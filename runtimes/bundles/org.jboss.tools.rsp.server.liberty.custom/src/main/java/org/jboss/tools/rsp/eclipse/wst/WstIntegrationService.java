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
import org.jboss.tools.rsp.server.spi.model.IServerModel;

/**
 * Owns WST integration components and installs the lifecycle strategy.
 */
public final class WstIntegrationService {

	private final ServerHandleRegistry registry;
	private final WSTServerFacade facade;
	private final WstServerLifecycleStrategy lifecycleStrategy;
	private final WstModelAdapter adapter;

	public WstIntegrationService() {
		this(new ServerHandleRegistry());
	}

	public WstIntegrationService(ServerHandleRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.adapter = new WstModelAdapter();
		this.facade = new WSTServerFacade(this.registry, this.adapter);
		this.lifecycleStrategy = new WstServerLifecycleStrategy(this.facade);
	}

	public WSTServerFacade getFacade() {
		return facade;
	}

	public ServerHandleRegistry getRegistry() {
		return registry;
	}

	public WstModelAdapter getAdapter() {
		return adapter;
	}

	public WstServerLifecycleStrategy getLifecycleStrategy() {
		return lifecycleStrategy;
	}

	public void install(IServerModel model) {
		if (model != null) {
			model.setServerLifecycleStrategy(lifecycleStrategy);
		}
	}

	public void uninstall(IServerModel model) {
		if (model != null) {
			model.setServerLifecycleStrategy(DefaultServerLifecycleStrategy.INSTANCE);
		}
	}

	public void dispose(IServerModel model) {
		uninstall(model);
		facade.dispose();
	}
}
