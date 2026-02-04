/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.model;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.cabutchei.rsp.api.RSPClient;
import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.api.dao.ServerState;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallRegistry;
import com.github.cabutchei.rsp.eclipse.jdt.launching.VMInstallRegistry;
import com.github.cabutchei.rsp.runtime.core.RuntimeCoreActivator;
import com.github.cabutchei.rsp.runtime.core.model.IDownloadRuntimesModel;
import com.github.cabutchei.rsp.secure.model.ISecureStorageProvider;
import com.github.cabutchei.rsp.server.CapabilityManagement;
import com.github.cabutchei.rsp.server.discovery.DiscoveryPathModel;
import com.github.cabutchei.rsp.server.discovery.serverbeans.ServerBeanTypeManager;
import com.github.cabutchei.rsp.server.filewatcher.FileWatcherService;
import com.github.cabutchei.rsp.server.jobs.JobManager;
import com.github.cabutchei.rsp.server.secure.SecureStorageGuardian;
import com.github.cabutchei.rsp.server.spi.discovery.IDiscoveryPathModel;
import com.github.cabutchei.rsp.server.spi.discovery.IServerBeanTypeManager;
import com.github.cabutchei.rsp.server.spi.filewatcher.IFileWatcherService;
import com.github.cabutchei.rsp.server.spi.jobs.IJobManager;
import com.github.cabutchei.rsp.server.spi.model.ICapabilityManagement;
import com.github.cabutchei.rsp.server.spi.model.IDataStoreModel;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModelListener;
import com.github.cabutchei.rsp.server.spi.model.ServerModelListenerAdapter;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.workspace.IProjectsManager;
import com.github.cabutchei.rsp.server.workspace.DefaultProjectsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerManagementModel implements IServerManagementModel {
	private static final Logger LOG = LoggerFactory.getLogger(ServerManagementModel.class);

	private static final String SECURESTORAGE_DIRECTORY = "securestorage";

	private ISecureStorageProvider secureStorage;
	private ICapabilityManagement capabilities;

	private IDiscoveryPathModel rpm;
	private IServerBeanTypeManager serverBeanTypeManager;
	private IServerModel serverModel;
	private IVMInstallRegistry vmModel;
	private IFileWatcherService fileWatcherService;
	private IDownloadRuntimesModel downloadRuntimeModel;
	private IJobManager jobManager;
	private IProjectsManager projectsManager;
	private IDataStoreModel fDataStoreModel;
	
	public ServerManagementModel(IDataStoreModel dataLocation) {
		this.capabilities = createCapabilityManagement();
		this.fDataStoreModel = dataLocation;
		File dLocFile = dataLocation.getDataLocation();
		this.secureStorage = createSecureStorageProvider(getSecureStorageFile(dLocFile), capabilities);
		this.rpm = createDiscoveryPathModel();
		this.serverBeanTypeManager = createServerBeanTypeManager();
		this.serverModel = createServerModel();
		this.vmModel = createVMInstallRegistry();
		this.vmModel.addActiveVM();
		this.fileWatcherService = createFileWatcherService();
		this.fileWatcherService.start();
		this.downloadRuntimeModel = createDownloadRuntimesModel();
		this.jobManager = createJobManager();
		this.projectsManager = createProjectsManager();
	}
	
	@Override
	public IJobManager getJobManager() {
		return this.jobManager;
	}

	@Override
	public IProjectsManager getProjectsManager() {
		return this.projectsManager;
	}
	
	@Override
	public ISecureStorageProvider getSecureStorageProvider() {
		return secureStorage;
	}

	@Override
	public IDiscoveryPathModel getDiscoveryPathModel() {
		return rpm;
	}

	@Override
	public IServerBeanTypeManager getServerBeanTypeManager() {
		return serverBeanTypeManager;
	}

	@Override
	public IServerModel getServerModel() {
		return serverModel;
	}

	@Override
	public IVMInstallRegistry getVMInstallModel() {
		return vmModel;
	}

	@Override
	public ICapabilityManagement getCapabilityManagement() {
		return capabilities;
	}

	@Override
	public IFileWatcherService getFileWatcherService() {
		return fileWatcherService;
	}

	@Override
	public IDownloadRuntimesModel getDownloadRuntimeModel() {
		return downloadRuntimeModel;
	}
	
	@Override
	public void clientRemoved(RSPClient client) {
		capabilities.clientRemoved(client);
		if( secureStorage instanceof SecureStorageGuardian ) 
			((SecureStorageGuardian)secureStorage).removeClient(client);
	}

	public void clientAdded(RSPClient client) {
		capabilities.clientAdded(client);
	}

	private File getSecureStorageFile(File dataLocation) {
		File secure = new File(dataLocation, SECURESTORAGE_DIRECTORY);
		return secure;
	}

	/*
	 * Following methods are for tests / subclasses to override. This is not advised
	 * for clients / extenders, since this class appears to behave as a singleton.
	 */
	protected ISecureStorageProvider createSecureStorageProvider(File file, ICapabilityManagement mgmt) {
		return new SecureStorageGuardian(file, mgmt);
	}

	protected ICapabilityManagement createCapabilityManagement() {
		return new CapabilityManagement();
	}

	protected IDiscoveryPathModel createDiscoveryPathModel() {
		return new DiscoveryPathModel();
	}

	protected VMInstallRegistry createVMInstallRegistry() {
		return new VMInstallRegistry();
	}

	protected IServerModel createServerModel() {
		return new ServerModel(this);
	}

	protected IServerBeanTypeManager createServerBeanTypeManager() {
		return new ServerBeanTypeManager();
	}

	protected IDownloadRuntimesModel createDownloadRuntimesModel() {
		IDownloadRuntimesModel ret = RuntimeCoreActivator.createDownloadRuntimesModel();
		ret.setDataLocation(this.fDataStoreModel.getDataLocation());
		return ret;
	}

	protected IFileWatcherService createFileWatcherService() {
		return new FileWatcherService();
	}

	protected IJobManager createJobManager() {
		return new JobManager();
	}

	protected IProjectsManager createProjectsManager() {
		return new DefaultProjectsManager();
	}

	@Override
	public void dispose() {
		shutdownAllServers();
		if( this.jobManager != null ) {
			this.jobManager.shutdown();
		}
	}

	protected void shutdownAllServers() {
		if( getNotStoppedServers().isEmpty())
			return;
		
		shutdownServers(getStartedServers(), false);
		shutdownServers(getNotStoppedServers(), true);
	}
	
	protected void shutdownServers(List<IServer> list, boolean force) {
		if( list.isEmpty()) 
			return;
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool(list.size());
		CountDownLatch latch = new CountDownLatch(list.size());
		Iterator<IServer> it = list.iterator();
		while(it.hasNext()) {
			final IServer next = it.next();
			submitShutdownServerRequest(next, force, threadExecutor, latch);
		}
		try {
			boolean result = latch.await(60000, TimeUnit.MILLISECONDS);
			if( !result ) {
				LOG.error("Waiting too long for shutdown of servers during RSP shutdown");
			}
		} catch(InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		threadExecutor.shutdown();
	}
	
	private void submitShutdownServerRequest(IServer next, boolean force, 
			ExecutorService threadExecutor, CountDownLatch latch) {
		threadExecutor.submit(() -> {
			IServerModelListener l = new ServerModelListenerAdapter() {
				@Override
				public void serverStateChanged(ServerHandle server, ServerState state) {
					if( server.getId().equals(next.getId()) && state.getState() == ServerManagementAPIConstants.STATE_STOPPED) {
						serverModel.removeServerModelListener(this);
						latch.countDown();
					}
				}
			};
			serverModel.addServerModelListener(l);
			next.getDelegate().stop(force);
		});
	}
	
	
	private List<IServer> getStartedServers() {
		return serverModel.getServers().values().stream()
			.filter(s -> s.getDelegate().getServerRunState() == ServerManagementAPIConstants.STATE_STARTED)
			.collect(Collectors.toList());
	}
	private List<IServer> getNotStoppedServers() {
		return serverModel.getServers().values().stream()
				.filter(s -> s.getDelegate().getServerRunState() != ServerManagementAPIConstants.STATE_STOPPED)
				.collect(Collectors.toList());
	}

	@Override
	public IDataStoreModel getDataStoreModel() {
		return fDataStoreModel;
	}
}
