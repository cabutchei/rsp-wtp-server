package com.github.cabutchei.rsp.server.eap.impl;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.wst.WstServerTypeHandler;
import com.github.cabutchei.rsp.server.eap.servertype.IEapServerAttributes;

final class EapServerTypeHandler implements WstServerTypeHandler {
	@Override
	public boolean handles(String serverTypeId) {
		return IEapServerAttributes.EAP_70_SERVER_TYPE.equals(serverTypeId);
	}

	@Override
	public void configureServer(IServerWorkingCopy server,
			IRuntimeWorkingCopy runtime,
			Map<String, Object> attributes,
			IProgressMonitor monitor) throws CoreException {
		String configFile = getStringAttribute(attributes, IEapServerAttributes.CONFIG_FILE,
				IEapServerAttributes.CONFIG_FILE_DEFAULT);
		server.setAttribute(IEapServerAttributes.CONFIG_FILE, configFile);

		String baseDir = getStringAttribute(attributes, IEapServerAttributes.BASE_DIRECTORY,
				IEapServerAttributes.BASE_DIRECTORY_DEFAULT);
		server.setAttribute(IEapServerAttributes.BASE_DIRECTORY, baseDir);
	}

	private String getStringAttribute(Map<String, Object> attributes, String key, String defaultValue) {
		if (attributes == null || key == null) {
			return defaultValue;
		}
		Object value = attributes.get(key);
		return value instanceof String ? (String) value : defaultValue;
	}
}
