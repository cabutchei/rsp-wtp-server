package com.github.cabutchei.rsp.eclipse.wst;

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
