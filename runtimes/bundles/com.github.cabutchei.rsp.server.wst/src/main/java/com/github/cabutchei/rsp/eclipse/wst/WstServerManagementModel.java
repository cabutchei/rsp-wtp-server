package com.github.cabutchei.rsp.eclipse.wst;

import java.util.Collections;
import java.util.Objects;

import com.github.cabutchei.rsp.server.model.ServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IDataStoreModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;

import com.github.cabutchei.rsp.eclipse.workspace.EclipseProjectImporter;
import com.github.cabutchei.rsp.eclipse.workspace.ProjectsManager;



public class WstServerManagementModel extends ServerManagementModel {
	private static final ThreadLocal<IWstIntegrationService> PENDING_INTEGRATION = new ThreadLocal<>();

	public WstServerManagementModel(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		// TODO: find a cleaner way to pass the integration service
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
