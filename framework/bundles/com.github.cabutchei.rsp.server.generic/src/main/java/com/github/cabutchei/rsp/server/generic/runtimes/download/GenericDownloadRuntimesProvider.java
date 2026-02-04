/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.generic.runtimes.download;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.cabutchei.rsp.api.DefaultServerAttributes;
import com.github.cabutchei.rsp.api.dao.CreateServerResponse;
import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.foundation.core.tasks.TaskModel;
import com.github.cabutchei.rsp.runtime.core.model.DownloadRuntime;
import com.github.cabutchei.rsp.runtime.core.model.IDownloadRuntimeRunner;
import com.github.cabutchei.rsp.runtime.core.model.IDownloadRuntimesProvider;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.runtimes.AbstractLicenseOnlyDownloadExecutor;
import com.github.cabutchei.rsp.server.spi.util.StatusConverter;

public class GenericDownloadRuntimesProvider implements IDownloadRuntimesProvider {
	private IServerManagementModel model;
	private String id;
	private DownloadRuntime[] dlrts;
	private String serverTypeId;

	public GenericDownloadRuntimesProvider(String id, String serverTypeId, DownloadRuntime[] dlrts) {
		this.id = id;
		this.serverTypeId = serverTypeId;
		this.dlrts = dlrts;
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	
	protected File getDataFolder() {
		return this.getModel().getDataStoreModel().getDataLocation();
	}

	@Override
	public DownloadRuntime[] getDownloadableRuntimes(IProgressMonitor monitor) {
		return this.dlrts;
	}

	@Override
	public IDownloadRuntimeRunner getDownloadRunner(DownloadRuntime dr) {
		return new AbstractLicenseOnlyDownloadExecutor(dr, getModel()) {
			@Override
			protected IStatus createServer(DownloadRuntime dlrt, String newHome, TaskModel tm) {
				String serverTypeId = getServerType();
				if( serverTypeId == null ) {
					return Status.CANCEL_STATUS;
				}
				// Now we have to somehow create this thing... ... ... 
				Set<String> serverIds = getServerModel().getServers().keySet();
				String suggestedId = new File(newHome).getName();
				String chosenId = getUniqueServerId(suggestedId, serverIds);
				
				Map<String,Object> attributes = new HashMap<>();
				if( new File(newHome).isFile()) {
					attributes.put(DefaultServerAttributes.SERVER_HOME_FILE, newHome);
				} else if( new File(newHome).isDirectory()) {
					attributes.put(DefaultServerAttributes.SERVER_HOME_DIR, newHome);
				}
				
				CreateServerResponse response = getServerModel().createServer(serverTypeId, chosenId, attributes);
				return StatusConverter.convert(response.getStatus());
			}
		};
	}
	
	private String getServerType() {
		return this.serverTypeId;
	}

	public IServerManagementModel getModel() {
		return model;
	}

	public void setModel(IServerManagementModel model) {
		this.model = model;
	}

}
