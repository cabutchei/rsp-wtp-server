package com.github.cabutchei.rsp.server.websphere.impl;

import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.servertype.impl.WebSphereContextRootSupport;

public class WebSphereServerDelegate extends AbstractWebSphereServerDelegate {

	public WebSphereServerDelegate(IServer server) {
		super(server);
	}

	@Override
	public String[] getDeploymentUrls(String strat, String baseUrl, String deployableOutputName,
			DeployableState ds) {
		return new WebSphereContextRootSupport().getDeploymentUrls(strat, baseUrl, deployableOutputName, ds);
	}
}
