package com.github.cabutchei.rsp.eclipse.wst.api;

import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.wst.model.WstServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IDataStoreModel;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModelFactory;

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
