/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.liberty.impl;

import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.launching.memento.JSONMemento;
import com.github.cabutchei.rsp.server.generic.servertype.GenericServerBehavior;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;
import com.github.cabutchei.rsp.server.tomcat.servertype.impl.LibertyContextRootSupport;

public class LibertyServerDelegate extends GenericServerBehavior implements IServerDelegate {

	public LibertyServerDelegate(IServer server, JSONMemento behaviorMemento) {
		super(server, behaviorMemento);
	}
	
	@Override
	public void setDependentDefaults(IServerWorkingCopy server) {
		setJavaLaunchDependentDefaults(server);
	}

	@Override
	public String[] getDeploymentUrls(String strat, String baseUrl, String deployableOutputName, DeployableState ds) {
		return new LibertyContextRootSupport().getDeploymentUrls(strat, baseUrl, deployableOutputName, ds); 
	}

}
