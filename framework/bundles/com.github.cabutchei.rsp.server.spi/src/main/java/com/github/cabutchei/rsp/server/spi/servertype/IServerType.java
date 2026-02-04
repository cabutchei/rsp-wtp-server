/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.servertype;

import com.github.cabutchei.rsp.api.RSPServer;
import com.github.cabutchei.rsp.api.dao.Attributes;
import com.github.cabutchei.rsp.api.dao.CreateServerWorkflowRequest;
import com.github.cabutchei.rsp.api.dao.ServerLaunchMode;
import com.github.cabutchei.rsp.api.dao.WorkflowResponse;

public interface IServerType {
	public String getId();
	public String getName();
	public String getDescription();
	public IServerDelegate createServerDelegate(IServer server);
	public Attributes getRequiredAttributes();
	public Attributes getOptionalAttributes();
	public boolean hasSecureAttributes();
	public Attributes getRequiredLaunchAttributes();
	public Attributes getOptionalLaunchAttributes();
	public ServerLaunchMode[] getLaunchModes();
	public WorkflowResponse createServerWorkflow(RSPServer server, CreateServerWorkflowRequest req);

}
