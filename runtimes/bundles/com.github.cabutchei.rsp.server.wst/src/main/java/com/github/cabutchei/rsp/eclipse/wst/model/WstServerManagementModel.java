package com.github.cabutchei.rsp.eclipse.wst.model;

import java.util.Collections;
import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.workspace.ProjectsManager;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstIntegrationService;
import com.github.cabutchei.rsp.eclipse.wst.wtp.WTPService;
import com.github.cabutchei.rsp.server.model.ServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IDataStoreModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.model.IWorkspaceModelCapability;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.spi.workspace.IWTPService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceInitializationService;



public class WstServerManagementModel extends ServerManagementModel implements IWorkspaceModelCapability {
	private static final ThreadLocal<IWstIntegrationService> PENDING_INTEGRATION = new ThreadLocal<>();
	private final IProjectsManager projectsManager;
	private final IWorkspaceInitializationService workspaceInitializationService;

	public WstServerManagementModel(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		// TODO: find a cleaner way to pass the integration service
		// this captureIntegration thing feels hacky, but it's a way to get the integration service into the createServerModel() method.
		super(captureIntegration(dataLocation, Objects.requireNonNull(wstIntegrationService, "wstIntegrationService")));
		PENDING_INTEGRATION.remove();
		IWstIntegrationService integration = Objects.requireNonNull(wstIntegrationService, "wstIntegrationService");
		IWTPService wtpService = new WTPService(integration.getFacade());
		this.projectsManager = new ProjectsManager(integration.getWorkspaceService(), wtpService, Collections.emptyList());
		this.workspaceInitializationService = integration.getWorkspaceInitializationService();
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
	public IProjectsManager getProjectsManager() {
		return projectsManager;
	}

	@Override
	public IWorkspaceInitializationService getWorkspaceInitializationService() {
		return workspaceInitializationService;
	}

	private static IDataStoreModel captureIntegration(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		PENDING_INTEGRATION.set(Objects.requireNonNull(wstIntegrationService, "wstIntegrationService"));
		return dataLocation;
	}
}
