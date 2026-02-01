/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import java.util.Collections;
import java.util.Objects;

import org.jboss.tools.rsp.server.model.ServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IDataStoreModel;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.workspace.IProjectsManager;

public class WstServerManagementModel extends ServerManagementModel {
	private static final ThreadLocal<IWstIntegrationService> PENDING_INTEGRATION = new ThreadLocal<>();

	public WstServerManagementModel(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		// this captureIntegration thing feels hacky, but it's a way to get the integration service into the createServerModel() method.
		super(captureIntegration(dataLocation, wstIntegrationService));
		PENDING_INTEGRATION.remove();
	}

	@Override
	protected IServerModel createServerModel() {
		IWstIntegrationService integration = PENDING_INTEGRATION.get();
		if (integration == null) {
			throw new IllegalStateException("WstIntegrationService must be provided before createServerModel()");
		}
		return new WSTServerModel(this, integration);
	}

	@Override
	protected IProjectsManager createProjectsManager() {
		IWstIntegrationService integration = PENDING_INTEGRATION.get();
		if (integration == null) {
			throw new IllegalStateException("WstIntegrationService must be provided before createProjectsManager()");
		}
		return new ProjectsManager(integration.getWorkspaceService(), integration.getFacade(),
				Collections.singletonList(new EclipseProjectImporter(integration.getWorkspaceService())));
	}

	private static IDataStoreModel captureIntegration(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		PENDING_INTEGRATION.set(Objects.requireNonNull(wstIntegrationService, "wstIntegrationService"));
		return dataLocation;
	}
}
