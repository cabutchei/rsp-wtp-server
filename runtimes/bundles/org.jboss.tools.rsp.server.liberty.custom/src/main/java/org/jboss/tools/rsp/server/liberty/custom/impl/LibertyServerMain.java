/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.liberty.custom.impl;

import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;
import org.jboss.tools.rsp.server.generic.GenericServerExtensionModel;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;


public class LibertyServerMain extends ServerManagementServerLauncher {

	public Object start(IApplicationContext context) throws Exception {
		LibertyServerMain.main(context.getArguments().get("application.args") != null ?
				(String[]) context.getArguments().get("application.args") : new String[0]);
		return IApplication.EXIT_OK;
	}

	public void stop() {
		// Nothing to do
	}

	public static void main(String[] args) throws Exception {
		LibertyServerMain instance = new LibertyServerMain(args[0]);
		LauncherSingleton.getDefault().setLauncher(instance);
		instance.launch();
		instance.shutdownOnInput();
	}
	
	public LibertyServerMain(String string) {
		super(string);
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
