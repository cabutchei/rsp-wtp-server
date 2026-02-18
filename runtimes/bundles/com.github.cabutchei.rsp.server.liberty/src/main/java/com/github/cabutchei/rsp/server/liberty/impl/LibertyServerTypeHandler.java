package com.github.cabutchei.rsp.server.liberty.impl;


import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.wst.api.WstServerTypeHandler;
import com.github.cabutchei.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;



final class LibertyServerTypeHandler implements WstServerTypeHandler {
	private static final String PROFILE_ATTRIBUTE = ILibertyServerAttributes.LIBERTY_PROFILE;

	@Override
	public boolean handles(String serverTypeId) {
		return serverTypeId != null
				&& serverTypeId.startsWith(ILibertyServerAttributes.LIBERTY_SERVER_TYPE_PREFIX);
	}

	@Override
	public void configureServer(IServerWorkingCopy server,
			IRuntimeWorkingCopy runtime,
			Map<String, Object> attributes,
			IProgressMonitor monitor) throws CoreException {
		String profileName = (String) attributes.get(PROFILE_ATTRIBUTE);
		server.setAttribute("serverName", profileName);
	}

}
