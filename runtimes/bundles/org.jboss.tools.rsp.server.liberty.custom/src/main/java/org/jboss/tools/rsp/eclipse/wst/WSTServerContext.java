package org.jboss.tools.rsp.eclipse.wst;



import java.util.List;
import java.util.Objects;

import org.eclipse.wst.server.core.internal.RuntimeWorkingCopy;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.debug.core.ILaunch;



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

    // public DeployableState[] getDeployableStates() {
    public List<DeployableState> getDeployableStates() {
        return this.facade.getDeployableStates(this.serverHandle);
    }

    public DeployableState getDeployableState(DeployableReference ref) {
        return this.facade.getDeployableState(this.serverHandle, ref);
    }

    public void start(String launchMode) throws CoreException {
        // this.facade.start(this.serverHandle, launchMode);
        this.facade.startSync(serverHandle, launchMode);
        org.eclipse.wst.server.core.IServer wstServer = this.facade.getWstServer(this.serverHandle.getId());
        RuntimeWorkingCopy copy = wstServer.getRuntime().createWorkingCopy().getAdapter(RuntimeWorkingCopy.class);
        org.eclipse.debug.core.ILaunch launch = wstServer.getLaunch();
        setLaunch(launch);
    }

    public IStatus canStart(String launchMode) {
        return this.facade.canStart(this.serverHandle, launchMode);
    }

    public void stop(boolean force) {
        this.facade.stop(this.serverHandle, force);
    }

    public IStatus canStop() {
        return this.facade.canStop(serverHandle);
    }

    private void setLaunch(org.eclipse.debug.core.ILaunch launch) {
        this.launch = new WstLaunchProxy(launch, this.facade.getAdapter());
    }

    public ILaunch getLaunch() {
        return this.launch;
    }

    // public DeployableReference[] getDeployableReferences() {
    //     return this.facade.getModules(serverHandle);
    // }
}
