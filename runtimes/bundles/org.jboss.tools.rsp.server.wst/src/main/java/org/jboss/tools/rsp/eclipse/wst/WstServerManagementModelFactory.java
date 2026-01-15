/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import java.util.Objects;

import org.jboss.tools.rsp.server.spi.model.IDataStoreModel;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModel;
import org.jboss.tools.rsp.server.spi.model.IServerManagementModelFactory;

public class WstServerManagementModelFactory implements IServerManagementModelFactory {
	private final IWstIntegrationService integrationService;

	public WstServerManagementModelFactory(IWstIntegrationService integrationService) {
		this.integrationService = Objects.requireNonNull(integrationService, "integrationService");
	}

	@Override
	public IServerManagementModel create(IDataStoreModel dataStoreModel) {
		return new WstServerManagementModel(dataStoreModel, integrationService);
	}
}
