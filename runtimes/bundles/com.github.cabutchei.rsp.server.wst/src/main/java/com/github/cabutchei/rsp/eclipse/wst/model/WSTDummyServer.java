// package com.github.cabutchei.rsp.eclipse.wst.model;

// import java.io.ByteArrayInputStream;
// import java.io.IOException;
// import java.io.InputStream;

// import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
// import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
// import java.util.Map;
// import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
// import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
// import com.github.cabutchei.rsp.eclipse.osgi.util.NLS;
// import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerManager;
// import com.github.cabutchei.rsp.eclipse.wst.core.WSTServerManager;
// import com.github.cabutchei.rsp.eclipse.wst.proxy.WstServerAdapter;
// import com.github.cabutchei.rsp.eclipse.wst.proxy.WstServerWorkingCopyAdapter;
// import com.github.cabutchei.rsp.launching.memento.IMemento;
// import com.github.cabutchei.rsp.server.ServerCoreActivator;
// import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
// import com.github.cabutchei.rsp.server.spi.model.IServerModel;
// import com.github.cabutchei.rsp.server.spi.servertype.IServer;
// import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
// import com.github.cabutchei.rsp.server.spi.servertype.IServerType;
// import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;

// import com.google.gson.JsonSyntaxException;

// // public class WSTDummyServer extends WSTBase implements IServer, IServerWorkingCopy {
// // public class WSTDummyServer extends WSTBase implements IServerWorkingCopy {
// public class WSTDummyServer extends WstServerWorkingCopyAdapter {
// 	// private final IServerModel serverModel;
// 	// private final IServerManagementModel managementModel;
// 	private IServerType serverType;
// 	private IServerDelegate delegate;

// 	public static WSTDummyServer createDummyServer(String json, IServerModel smodel,
// 			IServerManagementModel managementModel) throws CoreException {
// 		WSTDummyServer ds = new WSTDummyServer(smodel, managementModel);
// 		ds.loadFromJson(json);
// 		String serverTypeId = ds.getAttribute(WstServerAdapter.TYPE_ID, (String) null);
// 		IServerType type = smodel.getIServerType(serverTypeId);
// 		if (type == null) {
// 			throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0,
// 					NLS.bind("Server type not found: {0}", serverTypeId), null));
// 		}
// 		if (!(ds instanceof IServer)) {
// 			throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0,
// 					NLS.bind("WSTDummy must implement IServer", serverTypeId), null));
// 		}
// 		ds.setServerType(type);
// 		ds.setDelegate(type.createServerDelegate((IServer) ds));
// 		ds.loadFromJson(json);
// 		return ds;
// 	}

// 	public WSTDummyServer(IServerManagementModel managementModel, WSTServerManager serverManager) {
// 		super(
// 			serverManager.createServer(serverType, null, getMap(), managementModel), managementModel);
// 	}

// 	public void loadFromJson(String json) throws CoreException {
// 		try (InputStream in = new ByteArrayInputStream(json.getBytes())) {
// 			IMemento memento;
// 			try {
// 				memento = loadMemento(in);
// 			} catch (JsonSyntaxException jse) {
// 				throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0,
// 						NLS.bind("Parse error while reading server string: {0}", jse.getMessage()), null));
// 			}
// 			load(memento);
// 		} catch (IOException | RuntimeException e) {
// 			throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0,
// 					NLS.bind("Error while reading server string: {0}", e.getMessage()), e));
// 		}
// 	}

// 	@Override
// 	public String getName() {
// 		return getAttribute("name", (String) null);
// 	}

// 	@Override
// 	public String getId() {
// 		return getAttribute("id", (String) null);
// 	}

// 	@Override
// 	public String getTypeId() {
// 		if (serverType != null) {
// 			return serverType.getId();
// 		}
// 		return getAttribute(WstServerAdapter.TYPE_ID, (String) null);
// 	}

// 	@Override
// 	public IServerType getServerType() {
// 		return serverType;
// 	}

// 	public void setServerType(IServerType type) {
// 		this.serverType = type;
// 	}

// 	public void setDelegate(IServerDelegate delegate) {
// 		this.delegate = delegate;
// 	}

// 	@Override
// 	public IServerDelegate getDelegate() {
// 		return delegate;
// 	}

// 	@Override
// 	public String asJson(IProgressMonitor monitor) throws CoreException {
// 		throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID,
// 				"WSTDummyServer does not support JSON serialization"));
// 	}

// 	@Override
// 	public void load(IProgressMonitor monitor) throws CoreException {
// 		// No-op: dummy server is populated from JSON only.
// 	}

// 	@Override
// 	public void delete() throws CoreException {
// 		// No-op
// 	}

// 	@Override
// 	public IServerManagementModel getServerManagementModel() {
// 		return managementModel;
// 	}

// 	@Override
// 	public IServerModel getServerModel() {
// 		return serverModel;
// 	}

// 	@Override
// 	public <T> T getAdapter(Class<T> adapterType) {
// 		return null;
// 	}

// 	public Map<String, Object> getMap() {
// 		return map;
// 	}
// }
