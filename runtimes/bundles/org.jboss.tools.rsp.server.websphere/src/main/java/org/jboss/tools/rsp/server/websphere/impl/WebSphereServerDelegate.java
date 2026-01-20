/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.websphere.impl;


import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.StartServerResponse;
import org.jboss.tools.rsp.launching.memento.JSONMemento;
import org.jboss.tools.rsp.server.generic.servertype.GenericServerBehavior;
import org.jboss.tools.rsp.server.modeeel.WSTServerStreamListener;
import org.jboss.tools.rsp.server.publishing.WSTServerPublishStateModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
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


import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;




public class WebSphereServerDelegate extends GenericServerBehavior implements IServerDelegate {

	private static final String PROCESS_ID_KEY = "process.id.key";
	// private static final Logger LOG = LoggerFactory.getLogger(WebSphereServerDelegate.class);
	private WSTServerContext wstServerFacade;
	private final LaunchStreamAttacher launchStreamAttacher;

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
		try {
			launchStreamAttacher.reset();
			this.wstServerFacade.startAsync(mode);
			launchStreamAttacher.attach();
		} catch (CoreException e) {
			launchStreamAttacher.reset();
			s = StatusConverter.convert(e.getStatus());
			return new StartServerResponse(s, null);
		}
		return new StartServerResponse(StatusConverter.convert(Status.OK_STATUS), null);
	}

	@Override
	public IStatus canStart(String mode) {
		return this.wstServerFacade.canStart(mode);
	}

	@Override
	public IStatus stop(boolean force) {
		launchStreamAttacher.reset();
		this.wstServerFacade.stop(force);
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
					ServerManagementAPIConstants.STREAM_TYPE_SYSOUT);
			IStreamListener err = new WSTServerStreamListener(
					getServer(), getProcessId(all[i]), 
					ServerManagementAPIConstants.STREAM_TYPE_SYSERR);
			all[i].getStreamsProxy().getOutputStreamMonitor().addListener(out);
			all[i].getStreamsProxy().getErrorStreamMonitor().addListener(err);
		}
	}
}
