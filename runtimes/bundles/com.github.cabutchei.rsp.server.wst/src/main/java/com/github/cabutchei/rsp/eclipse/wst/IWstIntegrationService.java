package com.github.cabutchei.rsp.eclipse.wst;

import org.jboss.tools.rsp.server.spi.model.IServerModel;
import org.jboss.tools.rsp.server.spi.workspace.IWorkspaceService;

public interface IWstIntegrationService {
	WSTFacade getFacade();

	ServerHandleRegistry getRegistry();

	WstModelAdapter getAdapter();

	IWorkspaceService getWorkspaceService();

	void dispose(IServerModel model);

}
