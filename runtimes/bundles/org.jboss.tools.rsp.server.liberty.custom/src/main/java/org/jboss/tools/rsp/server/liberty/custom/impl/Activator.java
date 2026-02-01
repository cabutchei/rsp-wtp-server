/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.liberty.custom.impl;

import org.jboss.tools.rsp.server.spi.RSPExtensionBundle;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.ServerCoreActivator;
import org.jboss.tools.rsp.eclipse.wst.IWstIntegrationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends RSPExtensionBundle {
	public static final String BUNDLE_ID = "org.jboss.tools.rsp.server.liberty.custom";
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
	private static volatile IWstIntegrationService wstIntegration;
	private static volatile BundleContext bundleContext;

	public static synchronized IWstIntegrationService getWstIntegrationService() {
		if (wstIntegration == null) {
			wstIntegration = lookupIntegrationService();
		}
		return wstIntegration;
	}

	private static IWstIntegrationService lookupIntegrationService() {
		BundleContext context = bundleContext;
		if (context == null) {
			if (FrameworkUtil.getBundle(Activator.class) != null) {
				context = FrameworkUtil.getBundle(Activator.class).getBundleContext();
			}
		}
		if (context == null) {
			return null;
		}
		ServiceReference<IWstIntegrationService> ref = context.getServiceReference(IWstIntegrationService.class);
		if (ref == null) {
			return null;
		}
		return context.getService(ref);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		LOG.info("Bundle {} starting...", context.getBundle().getSymbolicName());
		bundleContext = context;
		addExtensions(ServerCoreActivator.BUNDLE_ID, context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("Bundle {} stopping...", context.getBundle().getSymbolicName());
		removeExtensions(ServerCoreActivator.BUNDLE_ID, context);
		bundleContext = null;
		wstIntegration = null;
	}

	@Override
	protected void addExtensions() {
		if (LauncherSingleton.getDefault() != null && LauncherSingleton.getDefault().getLauncher() != null) {
			ExtensionHandler.addExtensions(LauncherSingleton.getDefault().getLauncher().getModel());
		}
	}

	@Override
	protected void removeExtensions() {
		if (LauncherSingleton.getDefault() != null && LauncherSingleton.getDefault().getLauncher() != null) {
			ExtensionHandler.removeExtensions(LauncherSingleton.getDefault().getLauncher().getModel());
		}
	}

}
