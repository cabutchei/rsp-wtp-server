package com.github.cabutchei.rsp.eclipse.wst.bootstrap;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;

import com.github.cabutchei.rsp.eclipse.workspace.EclipseWorkspaceService;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerManager;
import com.github.cabutchei.rsp.eclipse.wst.api.WstServerManagementModelFactory;
import com.github.cabutchei.rsp.eclipse.wst.core.WSTServerManager;
import com.github.cabutchei.rsp.eclipse.wst.model.launch.ServerLaunchMonitor;
import com.github.cabutchei.rsp.server.ServerManagementServerLauncher;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceInitializationService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceInitializationRequest;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceInitializationSnapshot;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class WstBootstrapActivator implements BundleActivator {
	private static final Logger LOG = LoggerFactory.getLogger(WstBootstrapActivator.class);
	private static final long WORKSPACE_WAIT_MS = 60000;
	private static final boolean DEFAULT_AUTO_BUILDING = false;
	private IWstServerManager serverManager;
	private IWorkspaceService workspaceService;
	private IWorkspaceInitializationService workspaceInitializationService;

	@Override
	public void start(BundleContext context) {
		serverManager = new WSTServerManager();
		workspaceService = new EclipseWorkspaceService();
		workspaceInitializationService = workspaceService instanceof IWorkspaceInitializationService
				? (IWorkspaceInitializationService) workspaceService
				: null;
		ServerManagementServerLauncher.setServerManagementModelFactory(new WstServerManagementModelFactory(
				serverManager, workspaceService, workspaceInitializationService));
		bootstrap();
	}

	@Override
	public void stop(BundleContext context) {
		ServerLaunchMonitor.getInstance().stop();
		ServerManagementServerLauncher.clearServerManagementModelFactory();
		serverManager = null;
		workspaceInitializationService = null;
		workspaceService = null;
	}

	private void bootstrap() {
		IWorkspace workspace = waitForWorkspace();
		if (workspace == null) {
			LOG.warn("WST bootstrap skipped: workspace not available.");
			return;
		}
		applyWorkspaceBootstrapPolicy(workspace);
		configureGlobalWstAutoPublishing();
	}

	private IWorkspace waitForWorkspace() {
		IWorkspaceService workspaceSvc = workspaceService;
		if (workspaceSvc == null) {
			return null;
		}
		return workspaceSvc.awaitWorkspace(WORKSPACE_WAIT_MS);
	}

	private void applyWorkspaceBootstrapPolicy(IWorkspace workspace) {
		IWorkspaceInitializationService initializationService = workspaceInitializationService;
		if (initializationService != null) {
			IWorkspaceInitializationService init = initializationService;
			WorkspaceInitializationSnapshot snapshot = init.snapshot();
			WorkspaceInitializationRequest effective = snapshot == null ? null : snapshot.getEffectiveRequest();
			if (effective != null && effective.getAutoBuilding() != null) {
				com.github.cabutchei.rsp.eclipse.core.runtime.IStatus status = init.reapplyEffectivePolicy();
				if (status != null && !status.isOK()) {
					LOG.warn("WST bootstrap failed to reapply workspace initialization policy: {}", status.getMessage());
				}
				return;
			}
		}
		try {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(DEFAULT_AUTO_BUILDING);
			workspace.setDescription(description);
		} catch (CoreException ce) {
			LOG.warn("WST bootstrap failed to configure workspace auto-building", ce);
		}
	}

	private void configureGlobalWstAutoPublishing() {
		IWstServerManager manager = serverManager;
		if (manager == null) {
			LOG.warn("WST bootstrap skipped auto-publish configuration: server manager unavailable.");
			return;
		}
		manager.setGlobalAutoPublishing(false);
		manager.setAutoPublishingForAllServers(false);
		LOG.info("Disabled WTP global and per-server auto-publish settings.");
	}
}
