package com.github.cabutchei.rsp.eclipse.wst;

import org.eclipse.core.resources.IWorkspace;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class WstBootstrapActivator implements BundleActivator {
	private static final Logger LOG = LoggerFactory.getLogger(WstBootstrapActivator.class);
	private static final long LAUNCHER_WAIT_MS = 60000;
	private static final long WORKSPACE_WAIT_MS = 60000;
	private static final long WAIT_SLICE_MS = 200;
	private ServiceRegistration<IWstIntegrationService> registration;
	private IWstIntegrationService integrationService;
	private BundleContext bundleContext;

	@Override
	public void start(BundleContext context) {
		bundleContext = context;
		integrationService = new WstIntegrationService();
		registration = context.registerService(IWstIntegrationService.class, integrationService, null);
		ServerManagementServerLauncher.setServerManagementModelFactory(
			new WstServerManagementModelFactory(integrationService));
		Thread thread = new Thread(this::bootstrap, "WST Bootstrap");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void stop(BundleContext context) {
		ServerManagementServerLauncher.clearServerManagementModelFactory();
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
		integrationService = null;
		bundleContext = null;
	}

	private void bootstrap() {
		ServerManagementServerLauncher launcher = waitForLauncher();
		if (launcher == null) {
			LOG.warn("WST bootstrap skipped: launcher not available.");
			return;
		}
		if (!waitForWorkspace()) {
			LOG.warn("WST bootstrap skipped: workspace not available.");
			return;
		}
		IWstIntegrationService integration = integrationService;
		if (integration == null) {
			LOG.warn("WST bootstrap skipped: integration service unavailable.");
			return;
		}

		IServerManagementModel model = launcher.getModel();
		IServerModel serverModel = model == null ? null : model.getServerModel();
		if (serverModel instanceof WSTServerModel) {
			((WSTServerModel) serverModel).refreshServers();
		} else if (serverModel != null) {
			LOG.warn("WST bootstrap skipped server refresh: unexpected server model {}",
					serverModel.getClass().getName());
		} else {
			LOG.warn("WST bootstrap skipped server refresh: server model unavailable.");
		}
	}

	private ServerManagementServerLauncher waitForLauncher() {
		long deadline = System.currentTimeMillis() + LAUNCHER_WAIT_MS;
		ServerManagementServerLauncher launcher = LauncherSingleton.getDefault().getLauncher();
		while (launcher == null && System.currentTimeMillis() < deadline) {
			sleep(WAIT_SLICE_MS);
			launcher = LauncherSingleton.getDefault().getLauncher();
		}
		return launcher;
	}

	private boolean waitForWorkspace() {
		BundleContext context = bundleContext;
		if (context == null) {
			return false;
		}
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(context, IWorkspace.class, null);
		tracker.open();
		long deadline = System.currentTimeMillis() + WORKSPACE_WAIT_MS;
		try {
			IWorkspace workspace = tracker.getService();
			while (workspace == null && System.currentTimeMillis() < deadline) {
				sleep(WAIT_SLICE_MS);
				workspace = tracker.getService();
			}
			return workspace != null;
		} finally {
			tracker.close();
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
}
