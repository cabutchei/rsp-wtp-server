package com.github.cabutchei.rsp.server.liberty.impl;

import com.github.cabutchei.rsp.eclipse.wst.api.WstServerManagementModelFactory;
import com.github.cabutchei.rsp.server.LauncherSingleton;
import com.github.cabutchei.rsp.server.RSPFlags;
import com.github.cabutchei.rsp.server.ServerManagementServerLauncher;


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
