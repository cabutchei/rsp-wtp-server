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
import org.jboss.tools.rsp.eclipse.wst.WstIntegrationService;
import org.jboss.tools.rsp.eclipse.wst.WstServerManagementModel;
import org.jboss.tools.rsp.server.LauncherSingleton;
import org.jboss.tools.rsp.server.RSPFlags;
import org.jboss.tools.rsp.server.ServerManagementServerImpl;
import org.jboss.tools.rsp.server.ServerManagementServerLauncher;
import org.jboss.tools.rsp.server.generic.GenericServerExtensionModel;
import org.jboss.tools.rsp.server.persistence.DataLocationCore;


/** We are now skipping ServerCoreActivator and using our own Launcher. This allows us to have control over the server management model instance.*/
public class LibertyServerMain extends ServerManagementServerLauncher {

	public static void main(String[] args) throws Exception {
		LibertyServerMain instance = new LibertyServerMain("" + RSPFlags.DEFAULT_PORT);
		LauncherSingleton.getDefault().setLauncher(instance);
		instance.launch();
		instance.shutdownOnInput();
	}
	
	public LibertyServerMain(String port) {
		super(port);
	}

	@Override
	protected ServerManagementServerImpl createImpl() {
		DataLocationCore dlc = new DataLocationCore(this.portString);
		if (!dlc.isInUse()) {
			try {
				dlc.lock();
				WstIntegrationService integration = Activator.getWstIntegrationService();
				return new ServerManagementServerImpl(this, new WstServerManagementModel(dlc, integration));
			} catch (IOException ioe) {
				throw new RuntimeException("Error locking workspace", ioe);
			}
		}
		throw new RuntimeException("Workspace is locked. Please verify workspace is not in use, or, remove the .lock file at "
				+ dlc.getDataLocation().getAbsolutePath() + "/.lock");
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
