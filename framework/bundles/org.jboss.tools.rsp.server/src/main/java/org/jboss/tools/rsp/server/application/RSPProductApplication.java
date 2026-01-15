package org.jboss.tools.rsp.server.application;

/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/

import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSPProductApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(RSPProductApplication.class);
	private final CountDownLatch stopLatch = new CountDownLatch(1);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		LOG.info("Starting RSP Equinox application");
		context.applicationRunning();
		waitUntilStopped();
		return EXIT_OK;
	}

	private void waitUntilStopped() {
		try {
			stopLatch.await();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void stop() {
		stopLatch.countDown();
		ServerManagementServerLauncher launcher = LauncherSingleton.getDefault().getLauncher();
		if (launcher != null) {
			launcher.shutdown();
		}
		LOG.info("RSP Equinox application stopped");
	}
}
