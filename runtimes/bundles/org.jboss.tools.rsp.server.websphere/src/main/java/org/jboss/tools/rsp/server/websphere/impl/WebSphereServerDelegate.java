/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.websphere.impl;

import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;
import org.jboss.tools.rsp.server.servertype.impl.WebSphereContextRootSupport;
import org.jboss.tools.rsp.server.spi.servertype.IServer;

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
