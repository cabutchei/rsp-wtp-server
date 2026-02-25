package com.github.cabutchei.rsp.server.eap.adapter;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;
import com.github.cabutchei.rsp.server.eap.servertype.IEapServerAttributes;

public final class JBossRuntimeAdapter implements IJBossRuntimeAdapter {
	private final org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime jbossRuntime;

	public JBossRuntimeAdapter(org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime runtime) throws CoreException {
		this.jbossRuntime = runtime;
	}

	@Override
	public void setConfigurationFile(String file) throws CoreException {
		org.eclipse.wst.server.core.IRuntime runtime = jbossRuntime.getRuntime();
		if (!runtime.isWorkingCopy()) {
			throw new CoreException(new Status(IStatus.ERROR, "com.github.cabutchei.rsp.server.eap",
					"Expected WST RuntimeWorkingCopy but got "
							+ (runtime == null ? "null" : runtime.getClass().getName())));
		}
		((org.eclipse.wst.server.core.internal.RuntimeWorkingCopy) runtime)
				.setAttribute(IEapServerAttributes.CONFIG_FILE, file);
	}

	@Override
	public void setVM(IVMInstall vm) throws CoreException {
		org.eclipse.jdt.launching.IVMInstall nativeVm =
				com.github.cabutchei.rsp.eclipse.jdt.adapter.VMInstallAdapter.unwrap(vm);
		jbossRuntime.setVM(nativeVm);
	}
}
