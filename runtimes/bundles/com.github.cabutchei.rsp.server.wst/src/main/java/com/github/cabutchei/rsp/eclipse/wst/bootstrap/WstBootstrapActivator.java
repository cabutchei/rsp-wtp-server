package com.github.cabutchei.rsp.eclipse.wst.bootstrap;

import com.github.cabutchei.rsp.eclipse.workspace.EclipseWorkspaceService;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerCore;
import com.github.cabutchei.rsp.eclipse.wst.api.WstServerManagementModelFactory;
import com.github.cabutchei.rsp.eclipse.wst.core.WSTServerCore;
import com.github.cabutchei.rsp.eclipse.wst.model.launch.ServerLaunchMonitor;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.ServerManagementServerLauncher;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceInitializationService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class WstBootstrapActivator implements BundleActivator {
	private IWstServerCore serverManager;
	private IWorkspaceService workspaceService;
	private IWorkspaceInitializationService workspaceInitializationService;

	@Override
	public void start(BundleContext context) {
		serverManager = new WSTServerCore();
		workspaceService = new EclipseWorkspaceService();
		workspaceInitializationService = workspaceService instanceof IWorkspaceInitializationService
				? (IWorkspaceInitializationService) workspaceService
				: null;
		ServerManagementServerLauncher.setServerManagementModelFactory(new WstServerManagementModelFactory(
				serverManager, workspaceService, workspaceInitializationService));
		ServerCoreActivator.setLauncherFactory(WstServerManagementServerLauncher::new);
	}

	@Override
	public void stop(BundleContext context) {
		ServerLaunchMonitor.getInstance().stop();
		ServerCoreActivator.clearLauncherFactory();
		ServerManagementServerLauncher.clearServerManagementModelFactory();
		serverManager = null;
		workspaceInitializationService = null;
		workspaceService = null;
	}
}
