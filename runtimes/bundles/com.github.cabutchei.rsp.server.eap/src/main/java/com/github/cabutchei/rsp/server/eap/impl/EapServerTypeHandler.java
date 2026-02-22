package com.github.cabutchei.rsp.server.eap.impl;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.wst.api.WstServerTypeHandler;
import com.github.cabutchei.rsp.server.eap.servertype.IEapServerAttributes;

final class EapServerTypeHandler implements WstServerTypeHandler {
	private static final String CUSTOM_LAUNCH_SUBSYSTEM = "launch.my.local";

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

			String restartPattern = getStringAttribute(attributes, IEapServerAttributes.RESTART_FILE_PATTERN,
					IEapServerAttributes.RESTART_FILE_PATTERN_DEFAULT);
			boolean useDefaultRestartPattern = shouldUseDefaultRestartPattern(restartPattern);
			server.setAttribute(IEapServerAttributes.USE_DEFAULT_RESTART_FILE_PATTERN, useDefaultRestartPattern);
			if (useDefaultRestartPattern) {
				restartPattern = IEapServerAttributes.RESTART_FILE_PATTERN_DEFAULT;
			}
			server.setAttribute(IEapServerAttributes.RESTART_FILE_PATTERN, restartPattern);

			server.setAttribute(IEapServerAttributes.ATTACH_DEBUGGER, false);
			server.setAttribute(
					ControllableServerBehavior.PROPERTY_PREFIX + ILaunchServerController.SYSTEM_ID,
					CUSTOM_LAUNCH_SUBSYSTEM);
	}


	private String getStringAttribute(Map<String, Object> attributes, String key, String defaultValue) {
		if (attributes == null || key == null) {
			return defaultValue;
		}
		Object value = attributes.get(key);
		return value instanceof String ? (String) value : defaultValue;
	}

	private boolean shouldUseDefaultRestartPattern(String pattern) {
		if (pattern == null) {
			return true;
		}
		String trimmed = pattern.trim();
		if (trimmed.isEmpty()) {
			return true;
		}
		return IEapServerAttributes.RESTART_FILE_PATTERN_DEFAULT.equals(trimmed);
	}
}
