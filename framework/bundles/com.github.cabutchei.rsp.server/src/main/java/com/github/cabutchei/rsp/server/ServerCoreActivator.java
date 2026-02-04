/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server;

import com.github.cabutchei.rsp.eclipse.osgi.util.NLS;
import com.github.cabutchei.rsp.server.spi.model.DelayedExtensionManager;
import com.github.cabutchei.rsp.server.spi.model.DelayedExtensionManager.IDelayedExtension;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCoreActivator implements BundleActivator {

	public static final String BUNDLE_ID = "com.github.cabutchei.rsp.server";
	private static final Logger LOG = LoggerFactory.getLogger(ServerCoreActivator.class);
	private BundleContext context;

	@Override
	public void start(final BundleContext context) throws Exception {
		this.context = context;
		ShutdownExecutor.getExecutor().setHandler(() -> { performStop(); });
		startServer();
		LOG.debug(NLS.bind("{0} bundle started.", BUNDLE_ID));
	}

	public ServerManagementServerLauncher getLauncher() {
		return LauncherSingleton.getDefault().getLauncher();
	}

	private int getPort() {
		return RSPFlags.getServerPort();
	}

	private void startServer() {
		int port = getPort();
		ServerManagementServerLauncher launcher = null;
		
		try {
			launcher = new ServerManagementServerLauncher(""+port);
			LauncherSingleton.getDefault().setLauncher(launcher);
		} catch(RuntimeException re) {
			LOG.error("Unable to launch RSP server", re);
			performStop();
			return;
		}
		ServerManagementServerLauncher launcher2 = launcher;
		ClassLoader osgiContextClassLoader = Thread.currentThread().getContextClassLoader();
		if (osgiContextClassLoader == null) {
			osgiContextClassLoader = getClass().getClassLoader();
		}
		OsgiClassLoaderHolder.set(osgiContextClassLoader);
		Thread serverThread = new Thread(() -> {
				addDelayedExtensions();
				try {
					launcher2.launch(port);
				} catch (Exception e) {
					LOG.error("Unable to launch RSP server", e);
				}
			}, 
			"Launch RSP Server");
		// Use an OSGi-aware context classloader for request handling.
		serverThread.setContextClassLoader(osgiContextClassLoader);
		serverThread.start();
	}

	private void addDelayedExtensions() {
		IDelayedExtension[] addToModel = DelayedExtensionManager.getDefault().getDelayedExtensions();
		for( int i = 0; i < addToModel.length; i++ ) {
			addToModel[i].addExtensionsToModel();
		}
	}
	
	private void performStop() {
		try {
			context.getBundle(0).stop();
		} catch (BundleException e) {
			LOG.error(NLS.bind("Stopping bundle {0} failed.", BUNDLE_ID), e);
		}
	}
	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.debug(NLS.bind("{0} bundle stopped.", BUNDLE_ID));
	}
}
