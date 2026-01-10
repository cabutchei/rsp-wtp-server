/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.liberty.custom.impl;

import java.io.InputStream;

import org.jboss.tools.rsp.launching.memento.JSONMemento;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.ServerCoreActivator;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;
import org.jboss.tools.rsp.eclipse.wst.WstIntegrationService;
import org.jboss.tools.rsp.server.generic.GenericServerActivator;
import org.jboss.tools.rsp.server.generic.IServerBehaviorFromJSONProvider;
import org.jboss.tools.rsp.server.generic.IServerBehaviorProvider;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.api.dao.ServerType;
import org.jboss.tools.rsp.eclipse.osgi.util.NLS;
import org.jboss.tools.rsp.server.RSPFlags;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends GenericServerActivator {
	public static final String BUNDLE_ID = "org.jboss.tools.rsp.server.liberty.custom";
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
	private BundleContext context;
	private static WstIntegrationService wstIntegration;

	public static synchronized WstIntegrationService getWstIntegrationService() {
		if (wstIntegration == null) {
			wstIntegration = new WstIntegrationService();
		}
		return wstIntegration;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		LOG.info("Bundle {} starting...", context.getBundle().getSymbolicName());
		this.context = context;
		addExtensions(ServerCoreActivator.BUNDLE_ID, context);
		startServer();
		LOG.info(NLS.bind("{0} bundle started.", BUNDLE_ID));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("Bundle {} stopping...", context.getBundle().getSymbolicName());
		removeExtensions(ServerCoreActivator.BUNDLE_ID, context);
	}

	private void startServer() {
		int port = getPort();
		ServerManagementServerLauncher launcher = null;
		try {
			launcher = new LibertyServerMain(""+port);
			LauncherSingleton.getDefault().setLauncher(launcher);
		} catch(RuntimeException re) {
			LOG.error("Unable to launch RSP server", re);
			performStop();
			return;
		}
		ServerManagementServerLauncher launcher2 = launcher;
		new Thread(() -> {
				// addDelayedExtensions();
				try {
					launcher2.launch(port);
				} catch (Exception e) {
					LOG.error("Unable to launch RSP server", e);
				}
			}, 
			"Launch RSP Server")
		.start();
	}

	private void performStop() {
		try {
			context.getBundle(0).stop();
		} catch (BundleException e) {
			LOG.error(NLS.bind("Stopping bundle {0} failed.", BUNDLE_ID), e);
		}
	}

	private int getPort() {
		return RSPFlags.getServerPort();
	}

	@Override
	protected void removeExtensions() {
		IServerManagementModel model = LauncherSingleton.getDefault().getLauncher().getModel();
		if (wstIntegration != null) {
			wstIntegration.dispose(model.getServerModel());
			wstIntegration = null;
		}
		super.removeExtensions();
	}

	@Override
	protected String getBundleId() {
		return BUNDLE_ID;
	}

	@Override
	protected InputStream getServerTypeModelStream() {
		return getServerTypeModelStreamImpl();
	}

	public static final InputStream getServerTypeModelStreamImpl() {
		return Activator.class.getResourceAsStream("/servers.json");
	}

	protected IServerBehaviorFromJSONProvider getDelegateProvider() {
		return getDelegateProviderImpl();
	}

	public static IServerBehaviorFromJSONProvider getDelegateProviderImpl() {
		return new IServerBehaviorFromJSONProvider() {
			@Override
			public IServerBehaviorProvider loadBehaviorFromJSON(String serverTypeId, JSONMemento behaviorMemento) {
				return new IServerBehaviorProvider() {
					private ServerHandle toHandle(IServer s) {
						IServerType st = s.getServerType();
						return new ServerHandle(s.getId(), new ServerType(st.getId(), st.getName(), st.getDescription()));
					}
					@Override
					public IServerDelegate createServerDelegate(String typeId, IServer server) {
						if (typeId != null && typeId.startsWith(ILibertyServerAttributes.LIBERTY_SERVER_TYPE_PREFIX)) {
							LibertyServerDelegate del = new LibertyServerDelegate(server, behaviorMemento, new WSTServerContext(toHandle(server), getWstIntegrationService().getFacade()));
							// del.setWSTServerFacade(new WSTServerFacade(toHandle(server), getWstIntegrationService().getFacade()));
							return del;
						}
						return null;
					}
				};
			}
		};
	}

}
