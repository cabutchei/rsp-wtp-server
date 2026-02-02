package com.github.cabutchei.rsp.server.liberty.impl;

import org.jboss.tools.rsp.eclipse.wst.WstServerManagementModelFactory;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.RSPFlags;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;


public class LibertyServerMain extends ServerManagementServerLauncher {

	public static void main(String[] args) throws Exception {
		ServerManagementServerLauncher.setServerManagementModelFactory(
				new WstServerManagementModelFactory(Activator.getWstIntegrationService()));
		LibertyServerMain instance = new LibertyServerMain("" + RSPFlags.DEFAULT_PORT);
		LauncherSingleton.getDefault().setLauncher(instance);
		instance.launch();
		instance.shutdownOnInput();
	}
	
	public LibertyServerMain(String port) {
		super(port);
	}

	@Override
	public void launch(int port) throws Exception {
		ExtensionHandler.addExtensions(serverImpl.getModel());
		super.launch(port);
	}

}
