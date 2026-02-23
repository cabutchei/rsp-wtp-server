package com.github.cabutchei.rsp.eclipse.jdt.service;

import java.io.File;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.jdt.adapter.VMInstallAdapter;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallRegistry;

public class JdtVMService implements IJdtVMService {
	private static final String PLUGIN_ID = "com.github.cabutchei.rsp.jdt.eclipse";
	private final IVMInstallRegistry registry;

	public JdtVMService(IVMInstallRegistry registry) {
		this.registry = registry;
	}

	@Override
	public IVMInstall findOrCreateVMInstall(String installPath) throws CoreException {
		if (installPath == null || installPath.trim().isEmpty()) {
			return null;
		}
		File home = new File(installPath.trim());
		if (!home.exists() || !home.isDirectory()) {
			throw error("Configured VM install location does not exist or is not a directory: " + installPath);
		}
		IVMInstall existing = registry.findVMInstall(home);
		if (existing != null) {
			return existing;
		}
		org.eclipse.jdt.launching.IVMInstallType type = findCompatibleType(home);
		if (type == null) {
			throw error("No JDT VM install type recognized the configured VM install location: " + installPath);
		}
		String id = nextVmId(type, home);
		org.eclipse.jdt.launching.IVMInstall nativeVm = type.createVMInstall(id);
		nativeVm.setInstallLocation(home);
		nativeVm.setName("RSP " + (home.getName().isEmpty() ? id : home.getName()));
		IVMInstall wrapped = VMInstallAdapter.wrap(nativeVm, registry);
		registry.addVMInstall(wrapped);
		return wrapped;
	}

	private org.eclipse.jdt.launching.IVMInstallType findCompatibleType(File home) {
		for (org.eclipse.jdt.launching.IVMInstallType type : org.eclipse.jdt.launching.JavaRuntime.getVMInstallTypes()) {
			if (type == null) {
				continue;
			}
			org.eclipse.core.runtime.IStatus status = type.validateInstallLocation(home);
			if (status != null && status.isOK()) {
				return type;
			}
		}
		return null;
	}

	private String nextVmId(org.eclipse.jdt.launching.IVMInstallType type, File home) {
		String base = "rsp-" + sanitizeId(home.getName().isEmpty() ? "vm" : home.getName());
		String id = base;
		int i = 1;
		while (type.findVMInstall(id) != null) {
			id = base + "-" + (i++);
		}
		return id;
	}

	private String sanitizeId(String raw) {
		return raw.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private CoreException error(String msg) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, msg);
		return new CoreException(status);
	}
}
