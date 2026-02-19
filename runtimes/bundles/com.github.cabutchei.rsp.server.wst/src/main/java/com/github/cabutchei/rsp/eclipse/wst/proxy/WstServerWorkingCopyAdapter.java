package com.github.cabutchei.rsp.eclipse.wst.proxy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
import com.github.cabutchei.rsp.eclipse.wst.adapter.WstModelAdapter;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;

public class WstServerWorkingCopyAdapter implements IServerWorkingCopy, IServer {
	private final org.eclipse.wst.server.core.IServerWorkingCopy wstServerWorkingCopy;
	private final IServerManagementModel managementModel;
	private final IServerModel serverModel;
    private IServerDelegate delegate;

	public WstServerWorkingCopyAdapter(org.eclipse.wst.server.core.IServerWorkingCopy wstServerWorkingCopy,
			IServerManagementModel managementModel) {
		this.wstServerWorkingCopy = Objects.requireNonNull(wstServerWorkingCopy, "wstServerWorkingCopy");
		this.managementModel = managementModel;
		this.serverModel = managementModel.getServerModel();
	}

	// public WstServerWorkingCopyProxy(String id, org.eclipse.wst.server.core.IRuntime runtime, org.eclipse.wst.server.core.IServerType serverType) {
    //     this.wstServerWorkingCopy.loadAdapter(org.eclipse.wst.server.core.internal.ServerWorkingCopy, null).getServerType()
	// 	wch = new WorkingCopyHelper(this);
	// 	wch.setDirty(true);
	// 	if (serverType instanceof ServerType)
	// 		serverState = ((ServerType)serverType).getInitialState();
	// }

    @Override
	public String getName() {
        return wstServerWorkingCopy.getName();
    }

    @Override
    public String getId() {
        return wstServerWorkingCopy.getId();
    }

    @Override
    public void setAttribute(String key, int value) {
        wstServerWorkingCopy.setAttribute(key, value);
    }

    @Override
	public void setAttribute(String attributeName, boolean value) {
        wstServerWorkingCopy.setAttribute(attributeName, value);
    }

    @Override
	public void setAttribute(String attributeName, String value) {
        wstServerWorkingCopy.setAttribute(attributeName, value);
    }

    @Override
	public void setAttribute(String attributeName, List<String> value) {
        wstServerWorkingCopy.setAttribute(attributeName, value);
    }

    @Override
	public void setAttribute(String attributeName, Map value) {
        wstServerWorkingCopy.setAttribute(attributeName, value);
    }

    @Override
    public int getAttribute(String attributeName, int defaultValue) {
        return wstServerWorkingCopy.getAttribute(attributeName, defaultValue);
    }

    @Override
	public List<String> getAttribute(String attributeName, List<String> defaultValue) {
        return wstServerWorkingCopy.getAttribute(attributeName, defaultValue);
    }

    @Override
	public String getAttribute(String attributeName, String defaultValue) {
        return wstServerWorkingCopy.getAttribute(attributeName, defaultValue);
    }

    @Override
	public Map getAttribute(String attributeName, Map defaultValue) {
        return wstServerWorkingCopy.getAttribute(attributeName, defaultValue);
    }

    @Override
	public boolean getAttribute(String attributeName, boolean defaultValue) {
        return wstServerWorkingCopy.getAttribute(attributeName, defaultValue);
    }

	@Override
	public <T> T getAdapter(Class<T> adapterType) {
		if (adapterType == null) {
			return null;
		}
		if (adapterType.isInstance(this)) {
			return adapterType.cast(this);
		}
			if (adapterType.isInstance(wstServerWorkingCopy)) {
				return adapterType.cast(wstServerWorkingCopy);
			}
			try {
				Object adapted = wstServerWorkingCopy.loadAdapter(adapterType, null);
				return adapterType.cast(adapted);
			} catch (Exception e) {
				return null;
			}
		}

    @Override
    public IServer save(boolean force, IProgressMonitor monitor) throws CoreException {
        try {
            org.eclipse.wst.server.core.IServer server = wstServerWorkingCopy.save(force, new NullProgressMonitor());
            return new WstServerAdapter(server, managementModel);
        } catch (org.eclipse.core.runtime.CoreException e) {
            throw new CoreException(WstModelAdapter.toRspStatus(e.getStatus()));
        }
    }

    @Override
    public void save(IProgressMonitor monitor) throws CoreException {
        try {
            wstServerWorkingCopy.save(false, new NullProgressMonitor());
        } catch (org.eclipse.core.runtime.CoreException e) {
            throw new CoreException(WstModelAdapter.toRspStatus(e.getStatus()));
        }
    }

	@Override
	public String getTypeId() {
		IServerType serverType = getServerType();
		if (serverType != null) {
			return serverType.getId();
		}
		return wstServerWorkingCopy.getServerType() == null ? null : wstServerWorkingCopy.getServerType().getId();
	}

	@Override
	public IServerType getServerType() {
		return WstModelAdapter.toRspServerType(wstServerWorkingCopy.getServerType(), serverModel);
	}

	@Override
	public IServerDelegate getDelegate() {
		if (delegate != null) {
			return delegate;
		}
		IServerType serverType = getServerType();
		if (serverType == null) {
			return null;
		}
		delegate = serverType.createServerDelegate(this);
		return delegate;
	}

	@Override
	public IServerWorkingCopy createWorkingCopy() {
		return this;
	}

	@Override
	public String asJson(IProgressMonitor monitor) throws CoreException {
		throw new UnsupportedOperationException("asJson is not supported for server working copies");
	}

	@Override
	public void load(IProgressMonitor monitor) throws CoreException {
		// No-op: WST is the source of truth.
	}

	@Override
	public void delete() throws CoreException {
		throw new UnsupportedOperationException("delete is not supported for server working copies");
	}

	@Override
	public IServerManagementModel getServerManagementModel() {
		return managementModel;
	}

	@Override
	public IServerModel getServerModel() {
		return serverModel;
	}
}
