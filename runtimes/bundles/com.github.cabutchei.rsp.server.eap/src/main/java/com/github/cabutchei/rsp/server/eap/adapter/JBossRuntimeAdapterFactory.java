package com.github.cabutchei.rsp.server.eap.adapter;

import org.eclipse.core.runtime.IAdapterFactory;

import com.github.cabutchei.rsp.eclipse.wst.api.IWstRuntimeAdapter;

public class JBossRuntimeAdapterFactory implements IAdapterFactory {
	private static final Class<?>[] ADAPTERS = new Class<?>[] { IJBossRuntimeAdapter.class };

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == null || adaptableObject == null) {
			return null;
		}
		if (adapterType != IJBossRuntimeAdapter.class) {
			return null;
		}
		if (!(adaptableObject instanceof IWstRuntimeAdapter)) {
			return null;
		}
		IWstRuntimeAdapter runtime = (IWstRuntimeAdapter) adaptableObject;
		Object adapted = runtime.loadAdapter(org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime.class);
		if (!(adapted instanceof org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime)) {
			adapted = runtime.getAdapter(org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime.class);
		}
		if (!(adapted instanceof org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime)) {
			return null;
		}
		try {
			return (T) new JBossRuntimeAdapter((org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime) adapted);
		} catch (com.github.cabutchei.rsp.eclipse.core.runtime.CoreException e) {
			return null;
		}
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}
}
