package com.github.cabutchei.rsp.eclipse.wst.proxy;

import java.io.IOException;
import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.debug.core.model.IStreamMonitor;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IStreamsProxy;

public class WstStreamsProxy implements IStreamsProxy {
	private final org.eclipse.debug.core.model.IStreamsProxy wstProxy;
	private WstStreamMonitorProxy outputMonitor;
	private WstStreamMonitorProxy errorMonitor;

	public WstStreamsProxy(org.eclipse.debug.core.model.IStreamsProxy wstProxy) {
		this.wstProxy = Objects.requireNonNull(wstProxy, "wstProxy");
		this.outputMonitor = new WstStreamMonitorProxy(wstProxy.getOutputStreamMonitor());
		this.errorMonitor = new WstStreamMonitorProxy(wstProxy.getErrorStreamMonitor());
	}

	@Override
	public IStreamMonitor getErrorStreamMonitor() {
		return this.errorMonitor;
	}

	@Override
	public IStreamMonitor getOutputStreamMonitor() {
		return this.outputMonitor;
	}

	@Override
	public void write(String input) throws IOException {
		wstProxy.write(input);
	}
}
