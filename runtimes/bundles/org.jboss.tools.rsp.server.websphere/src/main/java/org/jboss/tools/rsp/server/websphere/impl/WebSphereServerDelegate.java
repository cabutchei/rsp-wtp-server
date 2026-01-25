/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.websphere.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.CommandLineDetails;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.ModuleState;
import org.jboss.tools.rsp.api.dao.ServerState;
import org.jboss.tools.rsp.api.dao.StartServerResponse;
import org.jboss.tools.rsp.eclipse.debug.core.ArgumentUtils;
import org.jboss.tools.rsp.launching.memento.JSONMemento;
import org.jboss.tools.rsp.launching.java.ILaunchModes;
import org.jboss.tools.rsp.launching.utils.LaunchingDebugProperties;
import org.jboss.tools.rsp.server.generic.servertype.GenericServerBehavior;
import org.jboss.tools.rsp.server.modeeel.WSTServerStreamListener;
import org.jboss.tools.rsp.server.publishing.WSTServerPublishStateModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.spi.servertype.IModuleStateProvider;
import org.jboss.tools.rsp.server.spi.servertype.IServerPublishModel;
import org.jboss.tools.rsp.server.spi.servertype.IServerWorkingCopy;
import org.jboss.tools.rsp.server.spi.util.StatusConverter;
import org.jboss.tools.rsp.server.servertype.impl.WebSphereContextRootSupport;
import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.eclipse.debug.core.ILaunch;
import org.jboss.tools.rsp.eclipse.debug.core.IStreamListener;
import org.jboss.tools.rsp.eclipse.debug.core.model.IProcess;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;

public class WebSphereServerDelegate extends GenericServerBehavior implements IServerDelegate, IModuleStateProvider {

	private static final String PROCESS_ID_KEY = "process.id.key";
	private static final long LAUNCH_WAIT_TIMEOUT_MS = 5000;
	private static final long DEBUG_PORT_WAIT_TIMEOUT_MS = 15000;
	// private static final Logger LOG = LoggerFactory.getLogger(WebSphereServerDelegate.class);
	private WSTServerContext wstServerFacade;
	private final LaunchStreamAttacher launchStreamAttacher;
	private volatile CompletableFuture<Integer> debugPortFuture = new CompletableFuture<>();

	public WebSphereServerDelegate(IServer server, JSONMemento behaviorMemento, WSTServerContext wstServerFacade) {
		super(server, behaviorMemento);
		this.wstServerFacade = wstServerFacade;
		this.launchStreamAttacher = new LaunchStreamAttacher(server.getId(), this::handleLaunchReady);
	}
	
	@Override
	public void setDependentDefaults(IServerWorkingCopy server) {
		setJavaLaunchDependentDefaults(server);
	}

	@Override
	public String[] getDeploymentUrls(String strat, String baseUrl, String deployableOutputName, DeployableState ds) {
		return new WebSphereContextRootSupport().getDeploymentUrls(strat, baseUrl, deployableOutputName, ds); 
	}

	@Override
	public IStatus canAddDeployable(DeployableReference ref) {
		return this.wstServerFacade.canAddDeployable(ref);
	}

	@Override
	public IStatus canRemoveDeployable(DeployableReference ref) {
		return this.wstServerFacade.canRemoveDeployable(ref);
	}

	@Override
	public IServerPublishModel createServerPublishModel() {
		return new WSTServerPublishStateModel(this, this.wstServerFacade, getFileWatcherService(), getFullPublishRequiredCallback());
	}

	public WSTServerContext getWSTServerFacade() {
		return wstServerFacade;
	}

	public void setWSTServerFacade(WSTServerContext wstServerFacade) {
		this.wstServerFacade = wstServerFacade;
	}

	@Override
	public IStatus canPublish() {
		return this.wstServerFacade.canPublish();
	}

	@Override
	public IStatus publish(int publishRequestType) {
		IStatus status = this.wstServerFacade.publish(publishRequestType);
		fireStateChanged(getServerState());
		return status;
	}

	@Override
	public int getServerRunState() {
		return this.wstServerFacade.getServerRunState();
	}

	@Override
	public ServerState getServerState() {
		return super.getServerState();
	}

	@Override
	public List<ModuleState> getModuleStates() {
		if (this.wstServerFacade == null) {
			return Collections.emptyList();
		}
		return this.wstServerFacade.getModuleStates();
	}

	@Override
	public String getMode() {
		return this.wstServerFacade.getMode();
	}

	@Override
	public StartServerResponse start(String mode) {
		IStatus stat = canStart(mode);
		org.jboss.tools.rsp.api.dao.Status s;
		if( !stat.isOK()) {
			s = StatusConverter.convert(stat);
			return new StartServerResponse(s, null);
		}
		CommandLineDetails details = new CommandLineDetails();
		try {
			if(ILaunchModes.DEBUG.equals(mode)) {
				addDebugDetails(WebSphereWstServerAccess.getDebugPort(wstServerFacade), details);
			}
			launchStreamAttacher.reset();
			launchStreamAttacher.attach();
			this.wstServerFacade.startAsync(mode);
		} catch (CoreException e) {
			launchStreamAttacher.reset();
			s = StatusConverter.convert(e.getStatus());
			return new StartServerResponse(s, null);
		}
		return new StartServerResponse(StatusConverter.convert(Status.OK_STATUS), details);
	}

	@Override
	public IStatus canStart(String mode) {
		return this.wstServerFacade.canStart(mode);
	}

	@Override
	public IStatus stop(boolean force) {
		launchStreamAttacher.reset();
		resetDebugPortFuture();
		this.wstServerFacade.stop(force);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus startModule(DeployableReference ref) {
		this.wstServerFacade.startModule(ref);
		return Status.OK_STATUS;
	}

	private void handleLaunchReady(ILaunch launch) {
		setStartLaunch(launch);
		addStreamListener(launch);
	}

	private void addStreamListener(ILaunch launch) {
		String ctime = "" + System.currentTimeMillis();
		IProcess[] all = launch.getProcesses();
		
		for( int i = 0; i < all.length; i++ ) {
			if( all[i].getAttribute(PROCESS_ID_KEY) != null ) {
				continue;
			}
			String pName = getServer().getTypeId() + ":" + getServer().getId()
					+ ":" + ctime + ":p" + i;
			all[i].setAttribute(PROCESS_ID_KEY, pName);
			IStreamListener out = new WSTServerStreamListener(
					getServer(), getProcessId(all[i]), 
					ServerManagementAPIConstants.STREAM_TYPE_SYSOUT, this::handleDebugPort);
			IStreamListener err = new WSTServerStreamListener(
					getServer(), getProcessId(all[i]), 
					ServerManagementAPIConstants.STREAM_TYPE_SYSERR, this::handleDebugPort);
			all[i].getStreamsProxy().getOutputStreamMonitor().addListener(out);
			all[i].getStreamsProxy().getErrorStreamMonitor().addListener(err);
		}
	}


	private CommandLineDetails awaitLaunchDetails(String mode) {
		ILaunch launch = launchStreamAttacher.awaitLaunchWithProcess(LAUNCH_WAIT_TIMEOUT_MS);
		if( launch == null ) {
			return null;
		}
		IProcess[] processes = launch.getProcesses();
		if( processes == null || processes.length == 0 ) {
			return null;
		}
		String cmdline = null;
		for( int i = 0; i < processes.length; i++ ) {
			String candidate = processes[i].getAttribute(IProcess.ATTR_CMDLINE);
			if( candidate != null && !candidate.isEmpty()) {
				cmdline = candidate;
				break;
			}
		}
		if( cmdline == null ) {
			return null;
		}
		CommandLineDetails details = new CommandLineDetails();
		details.setCmdLine(ArgumentUtils.parseArguments(cmdline));
		addDebugDetails(mode, details);
		return details;
	}

	private void addDebugDetails(int port, CommandLineDetails details) {
		if( port == 0 ) {
			return;
		}
		Map<String, String> props = details.getProperties();
		if( props == null ) {
			props = new HashMap<>();
			details.setProperties(props);
		}
		props.put(LaunchingDebugProperties.DEBUG_DETAILS_TYPE, LaunchingDebugProperties.DEBUG_DETAILS_TYPE_JAVA);
		props.put(LaunchingDebugProperties.DEBUG_DETAILS_HOST, "localhost");
		props.put(LaunchingDebugProperties.DEBUG_DETAILS_PORT, Integer.toString(port));
	}

	private void addDebugDetails(String mode, CommandLineDetails details) {
		if( !ILaunchModes.DEBUG.equals(mode)) {
			return;
		}
		Integer port = awaitDebugPort();
		if( port == null ) {
			return;
		}
		Map<String, String> props = details.getProperties();
		if( props == null ) {
			props = new HashMap<>();
			details.setProperties(props);
		}
		props.put(LaunchingDebugProperties.DEBUG_DETAILS_TYPE, LaunchingDebugProperties.DEBUG_DETAILS_TYPE_JAVA);
		props.put(LaunchingDebugProperties.DEBUG_DETAILS_HOST, "localhost");
		props.put(LaunchingDebugProperties.DEBUG_DETAILS_PORT, Integer.toString(port));
	}

	private Integer awaitDebugPort() {
		CompletableFuture<Integer> future = debugPortFuture;
		if( future == null ) {
			return null;
		}
		try {
			return future.get(DEBUG_PORT_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private void handleDebugPort(int port) {
		if( port <= 0 ) {
			return;
		}
		CompletableFuture<Integer> future = debugPortFuture;
		if( future != null && !future.isDone() ) {
			future.complete(port);
		}
	}

	private void resetDebugPortFuture() {
		debugPortFuture = new CompletableFuture<>();
	}
}
