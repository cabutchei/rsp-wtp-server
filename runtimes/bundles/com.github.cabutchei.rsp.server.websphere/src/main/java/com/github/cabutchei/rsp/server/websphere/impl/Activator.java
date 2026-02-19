package com.github.cabutchei.rsp.server.websphere.impl;

import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.LauncherSingleton;
import com.github.cabutchei.rsp.server.spi.RSPExtensionBundle;

import com.github.cabutchei.rsp.eclipse.wst.api.WstServerTypeHandlerRegistry;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Activator extends RSPExtensionBundle {
	public static final String BUNDLE_ID = "com.github.cabutchei.rsp.server.websphere";
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
	private static final WebSphereWstServerTypeHandler WST_HANDLER = new WebSphereWstServerTypeHandler();

	@Override
	public void start(BundleContext context) throws Exception {
		LOG.info("Bundle {} starting...", context.getBundle().getSymbolicName());
		WstServerTypeHandlerRegistry.register(WST_HANDLER);
		addExtensions(ServerCoreActivator.BUNDLE_ID, context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("Bundle {} stopping...", context.getBundle().getSymbolicName());
		removeExtensions(ServerCoreActivator.BUNDLE_ID, context);
		WstServerTypeHandlerRegistry.unregister(WST_HANDLER);
	}

	@Override
	protected void addExtensions() {
		if (LauncherSingleton.getDefault() != null
				&& LauncherSingleton.getDefault().getLauncher() != null) {
			ExtensionHandler.addExtensions(LauncherSingleton.getDefault().getLauncher().getModel());
		}
	}

	@Override
	protected void removeExtensions() {
		if (LauncherSingleton.getDefault() != null
				&& LauncherSingleton.getDefault().getLauncher() != null) {
			ExtensionHandler.removeExtensions(LauncherSingleton.getDefault().getLauncher().getModel());
		}
	}

}
