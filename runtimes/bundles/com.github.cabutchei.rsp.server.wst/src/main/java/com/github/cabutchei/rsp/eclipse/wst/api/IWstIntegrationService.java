package com.github.cabutchei.rsp.eclipse.wst.api;

import com.github.cabutchei.rsp.eclipse.wst.core.ServerHandleRegistry;
import com.github.cabutchei.rsp.eclipse.wst.core.WSTFacade;
import com.github.cabutchei.rsp.eclipse.wst.core.WSTServerManager;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceInitializationService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;

public interface IWstIntegrationService {
	WSTFacade getFacade();

	ServerHandleRegistry getRegistry();

	WSTServerManager getServerManager();

	IWorkspaceService getWorkspaceService();

	IWorkspaceInitializationService getWorkspaceInitializationService();

	void dispose(IServerModel model);

}
