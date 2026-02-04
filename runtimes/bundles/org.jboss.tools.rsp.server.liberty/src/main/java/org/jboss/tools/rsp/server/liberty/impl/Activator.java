/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.liberty.impl;

import java.io.InputStream;

import com.github.cabutchei.rsp.launching.memento.JSONMemento;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.generic.GenericServerActivator;
import com.github.cabutchei.rsp.server.generic.IServerBehaviorFromJSONProvider;
import com.github.cabutchei.rsp.server.generic.IServerBehaviorProvider;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends GenericServerActivator {
	public static final String BUNDLE_ID = "com.github.cabutchei.rsp.server.liberty";
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		LOG.info("Bundle {} starting...", context.getBundle().getSymbolicName());
		addExtensions(ServerCoreActivator.BUNDLE_ID, context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("Bundle {} stopping...", context.getBundle().getSymbolicName());
		removeExtensions(ServerCoreActivator.BUNDLE_ID, context);
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
					@Override
					public IServerDelegate createServerDelegate(String typeId, IServer server) {
						if (typeId != null && typeId.startsWith(ILibertyServerAttributes.LIBERTY_SERVER_TYPE_PREFIX)) {
							return new LibertyServerDelegate(server, behaviorMemento);
						}
						return null;
					}
				};
			}
		};
	}

}
