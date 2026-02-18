package com.github.cabutchei.rsp.eclipse.wst.api;



import java.util.List;
import java.util.Objects;

import com.github.cabutchei.rsp.api.dao.DeployableReference;
import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.api.dao.ModuleState;
import com.github.cabutchei.rsp.api.dao.ServerHandle;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.wst.core.WSTFacade;
import com.github.cabutchei.rsp.eclipse.wst.proxy.WstLaunchProxy;
import com.github.cabutchei.rsp.server.spi.servertype.IServerListener;



public class WSTServerContext {
    private final ServerHandle serverHandle;
    private final WSTFacade facade;
    private ILaunch launch;

    public WSTServerContext(ServerHandle serverHandle, WSTFacade facade) {
        this.serverHandle = Objects.requireNonNull(serverHandle, "serverHandle");
        this.facade = Objects.requireNonNull(facade, "facade");
    }

    public ServerHandle getServerHandle() {
        return serverHandle;
    }

    public IStatus addDeployable(DeployableReference reference) {
        return this.facade.addDeployable(reference, this.serverHandle);
    }

    public IStatus canAddDeployable(DeployableReference reference) {
        return this.facade.canAddDeployable(reference, this.serverHandle);
    }

    public IStatus removeDeployable(DeployableReference reference) {
        return this.facade.removeDeployable(reference, this.serverHandle);
    }

    public IStatus canRemoveDeployable (DeployableReference reference) {
        return this.facade.canRemoveDeployable(reference, this.serverHandle);
    }

    public IStatus publish(int publishRequestType) {
        return this.facade.publish(this.serverHandle, publishRequestType);
    }

    public IStatus canPublish() {
        return this.facade.canPublish(this.serverHandle);
    }

    public int getServerPublishState() {
        return this.facade.getServerPublishState(this.serverHandle);
    }

    public int getServerRunState() {
        return this.facade.getServerRunState(this.serverHandle);
    }

    public List<DeployableState> getDeployableStates() {
        return this.facade.getDeployableStates(this.serverHandle);
    }

    public List<ModuleState> getModuleStates() {
        return this.facade.getModuleStates(this.serverHandle);
    }

    public DeployableState getDeployableState(DeployableReference ref) {
        return this.facade.getDeployableState(this.serverHandle, ref);
    }

    public void start(String launchMode) throws CoreException {
        IStatus status = this.facade.start(serverHandle, launchMode);
        if (!status.isOK()) {
            throw new CoreException(status);
        }
        org.eclipse.wst.server.core.IServer wstServer = this.facade.getWstServer(this.serverHandle.getId());
        org.eclipse.debug.core.ILaunch launch = wstServer.getLaunch();
        setLaunch(launch);
    }

    public void startAsync(String launchMode) throws CoreException {
        this.facade.start(serverHandle, launchMode);
    }

    public IStatus canStart(String launchMode) {
        return this.facade.canStart(this.serverHandle, launchMode);
    }

    public void stop(boolean force) {
        this.facade.stop(serverHandle, force);
    }

    public IStatus canStop() {
        return this.facade.canStop(serverHandle);
    }

    public void startModule(DeployableReference ref) {
        this.facade.startModule(serverHandle, ref);
    }

    public void stopModule(DeployableReference ref) {
        this.facade.stopModule(serverHandle, ref);
    }

    public void addServerListener(IServerListener listener) {
        this.facade.addServerListener(serverHandle.getId(), listener);
    }

    private void setLaunch(org.eclipse.debug.core.ILaunch launch) {
        this.launch = new WstLaunchProxy(launch);
    }

    public ILaunch getLaunch() {
        return this.launch;
    }

	public String getMode() {
		return this.facade.getMode(serverHandle);
	}

    // public DeployableReference[] getDeployableReferences() {
    //     return this.facade.getModules(serverHandle);
    // }
}
