package org.jboss.tools.rsp.server.liberty.custom.impl;



import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;



public class EntryPoint implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        Thread.sleep(Long.MAX_VALUE); // Keep the application running. TODO: Replace with keepRunning
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
        // Nothing to do
    }

}
