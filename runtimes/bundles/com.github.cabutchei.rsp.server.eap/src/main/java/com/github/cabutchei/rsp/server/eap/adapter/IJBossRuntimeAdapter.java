package com.github.cabutchei.rsp.server.eap.adapter;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;

public interface IJBossRuntimeAdapter {
	// TODO: this should be moved to another class because the original interface doesn't have it
	void setConfigurationFile(String file) throws CoreException;
	void setVM(IVMInstall vm) throws CoreException;
}
