package com.github.cabutchei.rsp.eclipse.wst.model.delegate;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.DeployableReference;
import com.github.cabutchei.rsp.api.dao.ModuleState;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.debug.core.IStreamListener;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IProcess;
import com.github.cabutchei.rsp.eclipse.wst.api.WSTServerContext;
import com.github.cabutchei.rsp.eclipse.wst.model.launch.WstLaunchStreamAttacher;
import com.github.cabutchei.rsp.eclipse.wst.publishing.WSTServerPublishStateModel;
import com.github.cabutchei.rsp.eclipse.wst.stream.WSTServerStreamListener;
import com.github.cabutchei.rsp.server.model.AbstractServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.IModuleStateProvider;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerPublishModel;

public abstract class AbstractWstServerDelegate extends AbstractServerDelegate implements IModuleStateProvider {
	private static final String PROCESS_ID_KEY = "process.id.key";

	private WSTServerContext wstServerFacade;
	private final WstLaunchStreamAttacher launchStreamAttacher;

	protected AbstractWstServerDelegate(IServer server, WSTServerContext wstServerFacade) {
		super(server);
		this.wstServerFacade = wstServerFacade;
		this.launchStreamAttacher = new WstLaunchStreamAttacher(server.getId(), this::handleLaunchReady);
	}

	@Override
	public IStatus canAddDeployable(DeployableReference ref) {
		return wstServerFacade.canAddDeployable(ref);
	}

	@Override
	public IStatus canRemoveDeployable(DeployableReference ref) {
		return wstServerFacade.canRemoveDeployable(ref);
	}

	@Override
	public IServerPublishModel createServerPublishModel() {
		return new WSTServerPublishStateModel(this, wstServerFacade, getFileWatcherService(), getFullPublishRequiredCallback());
	}

	public WSTServerContext getWSTServerFacade() {
		return wstServerFacade;
	}

	public void setWSTServerFacade(WSTServerContext wstServerFacade) {
		this.wstServerFacade = wstServerFacade;
	}

	@Override
	public IStatus canPublish() {
		return wstServerFacade.canPublish();
	}

	@Override
	public IStatus publish(int publishRequestType) {
		IStatus status = wstServerFacade.publish(publishRequestType);
		if (status != null && status.isOK() && getServerPublishModel() instanceof WSTServerPublishStateModel) {
			((WSTServerPublishStateModel) getServerPublishModel()).markPublished();
		}
		fireStateChanged(getServerState());
		return status;
	}

	@Override
	public int getServerRunState() {
		return wstServerFacade.getServerRunState();
	}

	@Override
	public List<ModuleState> getModuleStates() {
		if (wstServerFacade == null) {
			return Collections.emptyList();
		}
		return wstServerFacade.getModuleStates();
	}

	@Override
	public String getMode() {
		return wstServerFacade.getMode();
	}

	@Override
	public IStatus canStart(String mode) {
		return wstServerFacade.canStart(mode);
	}

	@Override
	public IStatus stop(boolean force) {
		wstServerFacade.stop(force);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus startModule(DeployableReference ref) {
		wstServerFacade.startModule(ref);
		return Status.OK_STATUS;
	}

	protected final void prepareLaunchAttacher() {
		launchStreamAttacher.reset();
		launchStreamAttacher.attach();
	}

	protected final void resetLaunchAttacher() {
		launchStreamAttacher.reset();
	}

	protected final ILaunch awaitLaunchWithProcess(long timeoutMillis) {
		return launchStreamAttacher.awaitLaunchWithProcess(timeoutMillis);
	}

	protected void handleLaunchReady(ILaunch launch) {
		addLaunchStreamListeners(launch, false, null);
	}

	protected final void addLaunchStreamListeners(ILaunch launch, boolean skipAlreadyTagged, IntConsumer debugPortListener) {
		if (launch == null) {
			return;
		}
		String ctime = String.valueOf(System.currentTimeMillis());
		IProcess[] all = launch.getProcesses();
		if (all == null) {
			return;
		}
		for (int i = 0; i < all.length; i++) {
			IProcess process = all[i];
			if (process == null) {
				continue;
			}
			if (skipAlreadyTagged && process.getAttribute(PROCESS_ID_KEY) != null) {
				continue;
			}
			String pName = getServer().getTypeId() + ":" + getServer().getId() + ":" + ctime + ":p" + i;
			process.setAttribute(PROCESS_ID_KEY, pName);
			IStreamListener out = new WSTServerStreamListener(getServer(), getProcessId(process),
					ServerManagementAPIConstants.STREAM_TYPE_SYSOUT, debugPortListener);
			IStreamListener err = new WSTServerStreamListener(getServer(), getProcessId(process),
					ServerManagementAPIConstants.STREAM_TYPE_SYSERR, debugPortListener);
			process.getStreamsProxy().getOutputStreamMonitor().addListener(out);
			process.getStreamsProxy().getErrorStreamMonitor().addListener(err);
		}
	}
}
