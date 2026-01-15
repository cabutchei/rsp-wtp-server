/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import java.io.IOException;
import java.util.Objects;

import org.jboss.tools.rsp.eclipse.debug.core.model.IStreamMonitor;
import org.jboss.tools.rsp.eclipse.debug.core.model.IStreamsProxy;

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
