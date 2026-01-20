/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.websphere.impl;

import java.io.InputStream;

import org.jboss.tools.rsp.launching.memento.JSONMemento;
import org.jboss.tools.rsp.server.ServerCoreActivator;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;
import org.jboss.tools.rsp.eclipse.wst.IWstIntegrationService;
import org.jboss.tools.rsp.eclipse.wst.WstServerTypeHandlerRegistry;
import org.jboss.tools.rsp.server.generic.GenericServerActivator;
import org.jboss.tools.rsp.server.generic.IServerBehaviorFromJSONProvider;
import org.jboss.tools.rsp.server.generic.IServerBehaviorProvider;
import org.jboss.tools.rsp.server.servertype.impl.IWebSphereServerAttributes;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.api.dao.ServerType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Activator extends GenericServerActivator {
	public static final String BUNDLE_ID = "org.jboss.tools.rsp.server.websphere";
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
	private static volatile IWstIntegrationService wstIntegration;
	private static volatile BundleContext bundleContext;
	private static final WebSphereWstServerTypeHandler WST_HANDLER = new WebSphereWstServerTypeHandler();

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
		WstServerTypeHandlerRegistry.unregister(WST_HANDLER);
		bundleContext = null;
		wstIntegration = null;
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
						IWstIntegrationService integration = getWstIntegrationService();
						if (integration == null) {
							LOG.error("WST integration service not available.");
							return null;
						}
						if (typeId != null && typeId.startsWith(IWebSphereServerAttributes.WEBSPHERE_SERVER_TYPE_PREFIX)) {
							WebSphereServerDelegate del = new WebSphereServerDelegate(server, behaviorMemento, new WSTServerContext(toHandle(server), integration.getFacade()));
							return del;
						}
						return null;
					}
				};
			}
		};
	}

}
