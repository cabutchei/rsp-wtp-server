package com.github.cabutchei.rsp.eclipse.wst.stream;

import java.util.function.IntConsumer;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

import com.github.cabutchei.rsp.eclipse.debug.core.IStreamListener;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IStreamMonitor;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;

public class WSTServerStreamListener implements IStreamListener {

	// private static final Pattern DEBUG_PORT_PATTERN = Pattern.compile(
	// 		"Listening for transport .*address:\\s*(\\d+)");
	// private static final int BUFFER_MAX = 4096;

	private IServer server;
	private int streamType;
	private String processId;
	private final IntConsumer debugPortListener;
	private final StringBuilder buffer = new StringBuilder();

	public WSTServerStreamListener(IServer server, String processId, int type) {
		this(server, processId, type, null);
	}

	public WSTServerStreamListener(IServer server, String processId, int type, IntConsumer debugPortListener) {
		this.server = server;
		this.streamType = type;
		this.processId = processId;
		this.debugPortListener = debugPortListener;
	}

	@Override
	public void streamAppended(String text, IStreamMonitor monitor) {
		handleDebugPort(text);
		fireStreamAppended(server, streamType, text);
	}

	private void fireStreamAppended(IServer server2, int streamType, String text) {
		IServerModel serverModel = server.getServerManagementModel().getServerModel();
		serverModel.fireServerStreamAppended(server2, processId, streamType, text);
	}

	private void handleDebugPort(String text) {
		if( debugPortListener == null || text == null || text.isEmpty()) {
			return;
		}
		buffer.append(text);
		// Matcher matcher = DEBUG_PORT_PATTERN.matcher(buffer);
		// if( matcher.find()) {
		// 	try {
		// 		int port = Integer.parseInt(matcher.group(1));
		// 		debugPortListener.accept(port);
		// 	} catch (NumberFormatException e) {
		// 		// ignore
		// 	} finally {
		// 		buffer.setLength(0);
		// 	}
		// 	return;
		// }
		// if( buffer.length() > BUFFER_MAX ) {
		// 	buffer.delete(0, buffer.length() - BUFFER_MAX);
		// }
	}
	
}
