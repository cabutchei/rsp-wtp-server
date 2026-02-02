package com.github.cabutchei.rsp.server.websphere.impl;


import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.RSPFlags;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;

import com.github.cabutchei.rsp.eclipse.wst.WstServerManagementModelFactory;

public class WebSphereServerMain extends ServerManagementServerLauncher {

	public static void main(String[] args) throws Exception {
		ServerManagementServerLauncher.setServerManagementModelFactory(
				new WstServerManagementModelFactory(Activator.getWstIntegrationService()));
		WebSphereServerMain instance = new WebSphereServerMain("" + RSPFlags.DEFAULT_PORT);
		LauncherSingleton.getDefault().setLauncher(instance);
		instance.launch();
		instance.shutdownOnInput();
	}
	
	public WebSphereServerMain(String port) {
		super(port);
	}

	@Override
	public void launch(int port) throws Exception {
		ExtensionHandler.addExtensions(serverImpl.getModel());
		super.launch(port);
	}

}
