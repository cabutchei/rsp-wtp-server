package com.github.cabutchei.rsp.server.liberty.impl;

import java.util.Collections;
import java.util.List;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.DeployableReference;
import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.api.dao.ModuleState;
import com.github.cabutchei.rsp.api.dao.ServerState;
import com.github.cabutchei.rsp.api.dao.StartServerResponse;
import com.github.cabutchei.rsp.wst.server.model.WSTServerStreamListener;
import com.github.cabutchei.rsp.wst.server.model.publishing.WSTServerPublishStateModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.IModuleStateProvider;
import com.github.cabutchei.rsp.server.spi.servertype.IServerPublishModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;
import com.github.cabutchei.rsp.server.spi.util.StatusConverter;
import com.github.cabutchei.rsp.server.tomcat.servertype.impl.LibertyContextRootSupport;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.debug.core.IStreamListener;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IProcess;
import com.github.cabutchei.rsp.eclipse.wst.WSTServerContext;


public class LibertyServerDelegate extends AbstractLibertyServerDelegate implements IServerDelegate, IModuleStateProvider {

	private static final String PROCESS_ID_KEY = "process.id.key";
	// private static final Logger LOG = LoggerFactory.getLogger(LibertyServerDelegate.class);
	private WSTServerContext wstServerFacade;
	private final LaunchStreamAttacher launchStreamAttacher;

	public LibertyServerDelegate(IServer server, WSTServerContext wstServerFacade) {
		super(server);
		this.wstServerFacade = wstServerFacade;
		this.launchStreamAttacher = new LaunchStreamAttacher(server.getId(), this::handleLaunchReady);
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
		if (status != null && status.isOK() && getServerPublishModel() instanceof WSTServerPublishStateModel) {
			((WSTServerPublishStateModel) getServerPublishModel()).markPublished();
		}
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
		com.github.cabutchei.rsp.api.dao.Status s;
		if( !stat.isOK()) {
			s = StatusConverter.convert(stat);
			return new StartServerResponse(s, null);
		}
		try {
			launchStreamAttacher.reset();
			launchStreamAttacher.attach();
			this.wstServerFacade.startAsync(mode);
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
		this.wstServerFacade.stop(force);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus startModule(DeployableReference ref) {
		this.wstServerFacade.startModule(ref);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus stopModule(DeployableReference ref) {
		this.wstServerFacade.stopModule(ref);
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
