package com.github.cabutchei.rsp.eclipse.wst;

import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;

public interface IWstIntegrationService {
	WSTFacade getFacade();

	ServerHandleRegistry getRegistry();

	IWorkspaceService getWorkspaceService();

	void dispose(IServerModel model);

}
