package com.github.cabutchei.rsp.eclipse.wst.api;

import java.util.Map;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;

public interface IWstServerManager {
	IServer[] loadServers(IServerManagementModel managementModel);

	IServerWorkingCopy createServer(IServerType serverType, String id, Map<String, Object> attributes,
			IServerManagementModel model) throws CoreException;

	void setGlobalAutoPublishing(boolean enabled);
	default void updateServerStatus() {}
}
