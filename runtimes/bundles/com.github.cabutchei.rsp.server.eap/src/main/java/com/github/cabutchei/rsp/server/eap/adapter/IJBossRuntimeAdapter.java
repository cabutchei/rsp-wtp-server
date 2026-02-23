package com.github.cabutchei.rsp.server.eap.adapter;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;

public interface IJBossRuntimeAdapter {
	void setVM(IVMInstall vm) throws CoreException;
}
