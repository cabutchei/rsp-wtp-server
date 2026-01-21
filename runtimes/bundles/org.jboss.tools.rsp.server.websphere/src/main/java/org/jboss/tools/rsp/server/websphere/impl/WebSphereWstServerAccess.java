/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.websphere.impl;

import java.io.IOException;

import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.eclipse.wst.IWstIntegrationService;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;

import com.ibm.ws.ast.st.common.core.internal.config.IWASConfigModelHelper;
import com.ibm.ws.ast.st.v85.core.internal.util.ServerXmlFileHandler;
import com.ibm.ws.ast.st.v85.core.internal.WASServer;

final class WebSphereWstServerAccess {
	private WebSphereWstServerAccess() {
		// utility class
	}

	static ServerXmlFileHandler createServerXmlFileHandler(WSTServerContext context) throws IOException, CoreException  {
		WASServer server;
		server = getWASServer(context);
		return createServerXmlFileHandler(server.getWebSphereInstallPath(), server.getProfileName(), server.getBaseServerName());
	}

	static ServerXmlFileHandler createServerXmlFileHandler(String curWASInstallRoot, String profileName, String serverName) throws IOException {
		return ServerXmlFileHandler.create(curWASInstallRoot, profileName, serverName);
	}

	static int getDebugPortNum(WSTServerContext context) throws CoreException {
		WASServer server = getWASServer(context);
		return server.getDebugPortNum();
	}

	static IWASConfigModelHelper createLocalWASConfigHelper(WSTServerContext context) {
      try {
		return getWASServer(context).createLocalWASConfigHelper();
	  } catch (CoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	  }
	  return null;
	}

	static int getDebugPort(WSTServerContext context) {
		try {
			return createServerXmlFileHandler(context).getDebugPortNum();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return 0;
	}

	static WASServer getWASServer(WSTServerContext context) throws CoreException {
		if( context == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "Missing WST server context"));
		}
		IWstIntegrationService integration = Activator.getWstIntegrationService();
		if( integration == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "WST integration service unavailable"));
		}
		String serverId = context.getServerHandle().getId();
		org.eclipse.wst.server.core.IServer wstServer = integration.getFacade().getWstServer(serverId);
		if( wstServer == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "WST server not found for id " + serverId));
		}
		WASServer wasServer = (WASServer) wstServer.loadAdapter(WASServer.class, null);
		if( wasServer == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "Unable to load WASServer adapter"));
		}
		return wasServer;
	}
}
