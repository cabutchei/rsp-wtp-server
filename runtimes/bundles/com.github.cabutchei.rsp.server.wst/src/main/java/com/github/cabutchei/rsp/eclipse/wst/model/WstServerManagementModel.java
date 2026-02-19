package com.github.cabutchei.rsp.eclipse.wst.model;

import java.util.Collections;
import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.workspace.ProjectsManager;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstIntegrationService;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerManager;
import com.github.cabutchei.rsp.eclipse.wst.wtp.WTPService;
import com.github.cabutchei.rsp.server.model.ServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IDataStoreModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.model.IWorkspaceModelCapability;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.spi.workspace.IWTPService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceInitializationService;



public class WstServerManagementModel extends ServerManagementModel implements IWorkspaceModelCapability {
	private static final ThreadLocal<IWstServerManager> PENDING_SERVER_MANAGER = new ThreadLocal<>();
	private final IProjectsManager projectsManager;
	private final IWorkspaceInitializationService workspaceInitializationService;

	public WstServerManagementModel(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		super(captureDependencies(dataLocation, Objects.requireNonNull(wstIntegrationService, "wstIntegrationService")));
		PENDING_SERVER_MANAGER.remove();
		IWstIntegrationService integration = Objects.requireNonNull(wstIntegrationService, "wstIntegrationService");
		IWTPService wtpService = new WTPService();
		this.projectsManager = new ProjectsManager(integration.getWorkspaceService(), wtpService, Collections.emptyList());
		this.workspaceInitializationService = integration.getWorkspaceInitializationService();
	}

	@Override
	protected IServerModel createServerModel() {
		IWstServerManager serverManager = PENDING_SERVER_MANAGER.get();
		if (serverManager == null) {
			throw new IllegalStateException("WST dependencies must be provided before createServerModel()");
		}
		return new WSTServerModel(this, serverManager);
	}

	@Override
	public IProjectsManager getProjectsManager() {
		return projectsManager;
	}

	@Override
	public IWorkspaceInitializationService getWorkspaceInitializationService() {
		return workspaceInitializationService;
	}

	private static IDataStoreModel captureDependencies(IDataStoreModel dataLocation, IWstIntegrationService wstIntegrationService) {
		IWstIntegrationService integration = Objects.requireNonNull(wstIntegrationService, "wstIntegrationService");
		PENDING_SERVER_MANAGER.set(integration.getServerManager());
		return dataLocation;
	}
}
