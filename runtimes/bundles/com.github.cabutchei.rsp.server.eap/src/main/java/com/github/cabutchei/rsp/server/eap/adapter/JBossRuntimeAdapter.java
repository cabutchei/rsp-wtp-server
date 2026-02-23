package com.github.cabutchei.rsp.server.eap.adapter;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;

public final class JBossRuntimeAdapter implements IJBossRuntimeAdapter {
	private final org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime jbossRuntime;

	public JBossRuntimeAdapter(org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime runtime) throws CoreException {
		this.jbossRuntime = runtime;
	}

	@Override
	public void setVM(IVMInstall vm) throws CoreException {
		org.eclipse.jdt.launching.IVMInstall nativeVm =
				com.github.cabutchei.rsp.eclipse.jdt.adapter.VMInstallAdapter.unwrap(vm);
		jbossRuntime.setVM(nativeVm);
	}
}
