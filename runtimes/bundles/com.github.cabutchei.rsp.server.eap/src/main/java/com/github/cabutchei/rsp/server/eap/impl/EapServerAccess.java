package com.github.cabutchei.rsp.server.eap.impl;

import org.jboss.ide.eclipse.as.core.server.internal.v7.Wildfly8Server;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerDelegateAccess;
import com.github.cabutchei.rsp.eclipse.wst.adapter.WstModelAdapter;
import com.github.cabutchei.rsp.server.eap.servertype.launch.LaunchController;
import com.github.cabutchei.rsp.server.spi.servertype.IServerAttributes;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;


public class EapServerAccess implements IWstServerDelegateAccess<Wildfly8Server> {

	@Override
	public Class<Wildfly8Server> getDelegateType() {
		return Wildfly8Server.class;
	}

    public static LaunchController getLaunchController(IServerAttributes server) throws CoreException {
        IControllableServerBehavior behavior = getControllableServerBehavior(server);
        try {
            ISubsystemController controller = behavior.getController("launch.my.local");
            return (LaunchController) controller;
        } catch (org.eclipse.core.runtime.CoreException e) {
            IStatus status = WstModelAdapter.toRspStatus(e.getStatus());
            throw new CoreException(status);
        }
    }

    public static IControllableServerBehavior getControllableServerBehavior(IServerAttributes server) throws CoreException {
        org.eclipse.wst.server.core.IServer wtpServer = server.getAdapter(org.eclipse.wst.server.core.IServer.class);
        IControllableServerBehavior behavior =  JBossServerBehaviorUtils.getControllableBehavior(wtpServer);
        return behavior;
    }

}
