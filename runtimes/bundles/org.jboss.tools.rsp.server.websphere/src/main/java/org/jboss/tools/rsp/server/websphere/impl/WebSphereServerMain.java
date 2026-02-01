/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.websphere.impl;


import org.jboss.tools.rsp.eclipse.wst.WstServerManagementModelFactory;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.RSPFlags;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;

public class WebSphereServerMain extends ServerManagementServerLauncher {

	public static void main(String[] args) throws Exception {
		ServerManagementServerLauncher.setServerManagementModelFactory(
				new WstServerManagementModelFactory(Activator.getWstIntegrationService()));
		WebSphereServerMain instance = new WebSphereServerMain("" + RSPFlags.DEFAULT_PORT);
		LauncherSingleton.getDefault().setLauncher(instance);
		instance.launch();
		instance.shutdownOnInput();
	}
	
	public WebSphereServerMain(String port) {
		super(port);
	}

	@Override
	public void launch(int port) throws Exception {
		ExtensionHandler.addExtensions(serverImpl.getModel());
		super.launch(port);
	}

}
