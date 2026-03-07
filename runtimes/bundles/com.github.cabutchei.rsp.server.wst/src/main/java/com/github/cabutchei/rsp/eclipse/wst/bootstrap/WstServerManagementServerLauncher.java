package com.github.cabutchei.rsp.eclipse.wst.bootstrap;

import com.github.cabutchei.rsp.server.ServerManagementServerImpl;
import com.github.cabutchei.rsp.server.ServerManagementServerLauncher;
import com.github.cabutchei.rsp.server.persistence.DataLocationCore;

public class WstServerManagementServerLauncher extends ServerManagementServerLauncher {

	public WstServerManagementServerLauncher(String portString) {
		super(portString);
	}

	@Override
	public void launch(int port) throws Exception {
		startListening(port, serverImpl);
		this.getModel().getServerModel().loadServers();
	}

	// protected createServerManagementModel()

	@Override
	protected ServerManagementServerImpl createImpl() {
		DataLocationCore dlc = new DataLocationCore(this.portString);
		return new ServerManagementServerImpl(this, createServerManagementModel(dlc));
	}
}
