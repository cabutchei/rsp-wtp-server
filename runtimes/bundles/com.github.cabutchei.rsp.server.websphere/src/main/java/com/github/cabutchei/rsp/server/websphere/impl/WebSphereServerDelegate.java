package com.github.cabutchei.rsp.server.websphere.impl;

import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.eclipse.wst.WSTServerContext;
import com.github.cabutchei.rsp.server.servertype.impl.WebSphereContextRootSupport;

public class WebSphereServerDelegate extends AbstractWebSphereServerDelegate {

	public WebSphereServerDelegate(IServer server, WSTServerContext wstServerFacade) {
		super(server, wstServerFacade);
	}

	@Override
	public String[] getDeploymentUrls(String strat, String baseUrl, String deployableOutputName,
			DeployableState ds) {
		return new WebSphereContextRootSupport().getDeploymentUrls(strat, baseUrl, deployableOutputName, ds);
	}
}
