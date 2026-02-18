package com.github.cabutchei.rsp.server.liberty.impl;

import com.github.cabutchei.rsp.server.spi.RSPExtensionBundle;
import com.github.cabutchei.rsp.server.LauncherSingleton;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstIntegrationService;
import com.github.cabutchei.rsp.eclipse.wst.api.WstServerTypeHandlerRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends RSPExtensionBundle {
	public static final String BUNDLE_ID = "com.github.cabutchei.rsp.server.liberty";
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
	private static volatile IWstIntegrationService wstIntegration;
	private static volatile BundleContext bundleContext;
	private static final LibertyServerTypeHandler WST_HANDLER = new LibertyServerTypeHandler();

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
		WstServerTypeHandlerRegistry.register(WST_HANDLER);
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
