package com.github.cabutchei.rsp.server.websphere.impl;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;

import com.github.cabutchei.rsp.eclipse.wst.WstServerTypeHandler;
import com.github.cabutchei.rsp.server.servertype.impl.IWebSphereServerAttributes;

import com.ibm.ws.ast.st.common.core.internal.util.ProfileChangeHelper;
import com.ibm.ws.ast.st.v85.core.internal.WASServer;



final class WebSphereWstServerTypeHandler implements WstServerTypeHandler {
	private static final String PROFILE_ATTRIBUTE = IWebSphereServerAttributes.WEBSPHERE_PROFILE;

	@Override
	public boolean handles(String serverTypeId) {
		return serverTypeId != null
				&& serverTypeId.startsWith(IWebSphereServerAttributes.WEBSPHERE_SERVER_TYPE_PREFIX);
	}

	@Override
	public void configureServer(IServerWorkingCopy server,
			IRuntimeWorkingCopy runtime,
			Map<String, Object> attributes,
			IProgressMonitor monitor) throws CoreException {
		String profileName = attributes == null ? null : (String) attributes.get(PROFILE_ATTRIBUTE);
		// TODO: eventually use this to create let the user create a new profile
		// server.getRuntime().getAdapter(com.ibm.ws.st.core.internal.WebSphereRuntime.class).createServer()
		WASServer wasServer = ((WASServer) server.loadAdapter(WASServer.class, null));
		if( wasServer == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, 
					"Unable to load WebSphere server adapter"));
		}
		if( profileName == null || !Arrays.asList(wasServer.getProfileNames()).contains(profileName)) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "Profile does not exist"));
		}
		wasServer.setWebSphereProfileName(profileName);
		new ProfileChangeHelper().updateBaseServerForProfileChange(server, profileName);
	}
}
