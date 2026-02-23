package com.github.cabutchei.rsp.eclipse.jdt.service;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;

public interface IJdtVMService {
	IVMInstall findOrCreateVMInstall(String installPath) throws CoreException;
}
