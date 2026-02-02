package com.github.cabutchei.rsp.server.liberty.model;

import org.jboss.tools.rsp.eclipse.debug.core.IStreamListener;
import org.jboss.tools.rsp.eclipse.debug.core.model.IStreamMonitor;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;

public class WSTServerStreamListener implements IStreamListener {

	private IServer server;
	private int streamType;
	private String processId;

	public WSTServerStreamListener(IServer server, String processId, int type) {
		this.server = server;
		this.streamType = type;
		this.processId = processId;
	}

	@Override
	public void streamAppended(String text, IStreamMonitor monitor) {
		fireStreamAppended(server, streamType, text);
	}

	private void fireStreamAppended(IServer server2, int streamType, String text) {
		IServerModel serverModel = server.getServerManagementModel().getServerModel();
		serverModel.fireServerStreamAppended(server2, processId, streamType, text);
	}
	
}
