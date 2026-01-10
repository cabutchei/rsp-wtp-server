/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;

public interface IWstIntegrationService {
	WSTFacade getFacade();

	ServerHandleRegistry getRegistry();

	WstModelAdapter getAdapter();

	IWorkspaceService getWorkspaceService();

	WstServerLifecycleStrategy getLifecycleStrategy();

	void install(IServerModel model);

	void uninstall(IServerModel model);

	void dispose(IServerModel model);

	void refreshServers(IServerManagementModel model);
}
