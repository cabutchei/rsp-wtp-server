/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.liberty.custom.impl;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.launching.memento.JSONMemento;
import org.jboss.tools.rsp.server.generic.servertype.GenericServerBehavior;
import org.jboss.tools.rsp.server.publishing.WSTServerPublishStateModel;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.spi.servertype.IServerPublishModel;
import org.jboss.tools.rsp.server.spi.servertype.IServerWorkingCopy;
import org.jboss.tools.rsp.server.tomcat.servertype.impl.LibertyContextRootSupport;

import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.wst.WSTServerFacade;
import org.jboss.tools.rsp.server.ServerCoreActivator;

import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.*;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;

// import org.eclipse.wst.server.ui.internal.wizard.page.NewManualServerComposite;
// import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
// import com.ibm.ws.st.ui.internal.wizard.WebSphereServerComposite;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.ILaunch;

import java.util.function.Supplier;


public class LibertyServerDelegate extends GenericServerBehavior implements IServerDelegate {

	// private static final Logger LOG = LoggerFactory.getLogger(LibertyServerDelegate.class);
	private WSTServerFacade facade;

	public LibertyServerDelegate(IServer server, JSONMemento behaviorMemento, WSTServerFacade facade) {
		super(server, behaviorMemento);
		this.facade = facade;
	}
	
	@Override
	public void setDependentDefaults(IServerWorkingCopy server) {
		setJavaLaunchDependentDefaults(server);
	}

	@Override
	public String[] getDeploymentUrls(String strat, String baseUrl, String deployableOutputName, DeployableState ds) {
		return new LibertyContextRootSupport().getDeploymentUrls(strat, baseUrl, deployableOutputName, ds); 
	}

	@Override
	public IStatus canAddDeployable(DeployableReference ref) {
		return this.facade.canAddDeployable(ref, getServerHandle());
	}

	@Override
	public IStatus canRemoveDeployable(DeployableReference ref) {
		return this.facade.canRemoveDeployable(ref, getServerHandle());
	}

	@Override
	public IServerPublishModel createServerPublishModel() {
		Supplier<WSTServerFacade> facadeSupplier = () -> this.facade;
		return new WSTServerPublishStateModel(this, facadeSupplier, getFileWatcherService(), getFullPublishRequiredCallback());
	}

	// public IStatus canPublishDeployable(DeployableReference reference, int publishRequestType) {
	// 	return IStatus
	// }

	// @Override
	// protected void publishDeployable(DeployableReference reference, 
	// 		int publishRequestType, int modulePublishState) throws org.jboss.tools.rsp.eclipse.core.runtime.CoreException {
	// 	int syncState = getPublishController()
	// 			.publishModule(reference, publishRequestType, modulePublishState);
	// 	setDeployablePublishState(reference, syncState);
		
	// 	boolean serverStarted = getServerState().getState() == ServerManagementAPIConstants.STATE_STARTED;
	// 	int deployState = (serverStarted ? ServerManagementAPIConstants.
	// 			STATE_STARTED : ServerManagementAPIConstants.STATE_STOPPED);
	// 	setDeployableState(reference, deployState);
	// }

}


