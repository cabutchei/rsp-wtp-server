/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.liberty.custom.impl;

import java.io.IOException;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.jboss.tools.rsp.eclipse.wst.WstServerManagementModelFactory;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.RSPFlags;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;
import org.jboss.tools.rsp.server.generic.GenericServerExtensionModel;


/** We are now skipping ServerCoreActivator and using our own Launcher. This allows us to have control over the server management model instance.*/
public class LibertyServerMain extends ServerManagementServerLauncher {

	public static void main(String[] args) throws Exception {
		ServerManagementServerLauncher.setServerManagementModelFactory(
				new WstServerManagementModelFactory(Activator.getWstIntegrationService()));
		LibertyServerMain instance = new LibertyServerMain("" + RSPFlags.DEFAULT_PORT);
		LauncherSingleton.getDefault().setLauncher(instance);
		instance.launch();
		instance.shutdownOnInput();
	}
	
	public LibertyServerMain(String port) {
		super(port);
	}

	@Override
	public void launch(int port) throws Exception {
		GenericServerExtensionModel model = new GenericServerExtensionModel(serverImpl.getModel(), 
				Activator.getDelegateProviderImpl(),
				Activator.getServerTypeModelStreamImpl());
		model.registerExtensions();
		super.launch(port);
	}

}
