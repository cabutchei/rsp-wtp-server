package org.jboss.tools.rsp.server.liberty.custom.impl;



import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.wst.WstIntegrationService;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** We are now skipping ServerCoreActivator and using our own Launcher */

public class EntryPoint implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(EntryPoint.class);
	private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Override
    public Object start(IApplicationContext context) throws Exception {
        // TODO: should this be here or elsewhere?
        WstIntegrationService integration = Activator.getWstIntegrationService();
        IServerManagementModel model = LauncherSingleton.getDefault().getLauncher().getModel();
        IServerModel serverModel = model.getServerModel();
        integration.install(serverModel);
        IStatus wsStatus = integration.getWorkspaceService()
                .openWorkspace(integration.getWorkspaceService().getWorkspaceRoot());
        if (wsStatus != null && !wsStatus.isOK()) {
            LOG.warn("Workspace initialization failed: {}", wsStatus.getMessage());
        }
        IStatus importStatus = integration.getWorkspaceService().importAllProjects();
        if (importStatus != null && !importStatus.isOK()) {
            LOG.warn("Workspace import failed: {}", importStatus.getMessage());
        }
        integration.initialize(model);	
        context.applicationRunning();
		waitUntilStopped();
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
        // Nothing to do
    }

	private void waitUntilStopped() {
		try {
			stopLatch.await();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
}
