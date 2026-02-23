package com.github.cabutchei.rsp.eclipse.jdt;

import org.eclipse.core.runtime.Plugin;

import com.github.cabutchei.rsp.eclipse.jdt.service.EclipseJdtVMInstallRegistry;
import com.github.cabutchei.rsp.eclipse.jdt.service.IJdtVMService;
import com.github.cabutchei.rsp.eclipse.jdt.service.JdtVMService;

public class JDTPlugin extends Plugin {
	private static JDTPlugin plugin;
	private static final IJdtVMService VM_SERVICE = new JdtVMService(new EclipseJdtVMInstallRegistry());

	public JDTPlugin() {
		plugin = this;
	}

	public static JDTPlugin getDefault() {
		return plugin;
	}

	public static IJdtVMService getVMService() {
		return VM_SERVICE;
	}
}
