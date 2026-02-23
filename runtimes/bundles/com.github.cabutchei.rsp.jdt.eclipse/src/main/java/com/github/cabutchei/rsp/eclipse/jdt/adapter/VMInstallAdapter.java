package com.github.cabutchei.rsp.eclipse.jdt.adapter;

import java.io.File;
import java.net.URL;
import java.util.Map;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallRegistry;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallType;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMRunner;
import com.github.cabutchei.rsp.eclipse.jdt.launching.LibraryLocation;

public final class VMInstallAdapter implements IVMInstall {
	private final org.eclipse.jdt.launching.IVMInstall delegate;
	private final IVMInstallRegistry registry;

	public VMInstallAdapter(org.eclipse.jdt.launching.IVMInstall delegate, IVMInstallRegistry registry) {
		this.delegate = delegate;
		this.registry = registry;
	}

	public static IVMInstall wrap(org.eclipse.jdt.launching.IVMInstall vm, IVMInstallRegistry registry) {
		if (vm == null) {
			return null;
		}
		return new VMInstallAdapter(vm, registry);
	}

	public static org.eclipse.jdt.launching.IVMInstall unwrap(IVMInstall vm) {
		if (vm instanceof VMInstallAdapter) {
			return ((VMInstallAdapter) vm).delegate;
		}
		return null;
	}

	@Override
	public IVMRunner getVMRunner(String mode) {
		return null;
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public void setName(String name) {
		delegate.setName(name);
	}

	@Override
	public File getInstallLocation() {
		return delegate.getInstallLocation();
	}

	@Override
	public void setInstallLocation(File installLocation) {
		delegate.setInstallLocation(installLocation);
	}

	@Override
	public IVMInstallType getVMInstallType() {
		return new VMInstallTypeAdapter(delegate.getVMInstallType());
	}

	@Override
	public LibraryLocation[] getLibraryLocations() {
		return null;
	}

	@Override
	public void setLibraryLocations(LibraryLocation[] locations) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setJavadocLocation(URL url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public URL getJavadocLocation() {
		return null;
	}

	@Override
	public String[] getVMArguments() {
		if (delegate instanceof org.eclipse.jdt.launching.IVMInstall2) {
			String vmArgs = ((org.eclipse.jdt.launching.IVMInstall2) delegate).getVMArgs();
			return vmArgs == null || vmArgs.isBlank() ? null : vmArgs.trim().split("\\s+");
		}
		return null;
	}

	@Override
	@Deprecated
	public void setVMArguments(String[] vmArgs) {
		if (delegate instanceof org.eclipse.jdt.launching.IVMInstall2) {
			((org.eclipse.jdt.launching.IVMInstall2) delegate).setVMArgs(vmArgs == null ? null : String.join(" ", vmArgs));
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public String getVMArgs() {
		return delegate instanceof org.eclipse.jdt.launching.IVMInstall2
				? ((org.eclipse.jdt.launching.IVMInstall2) delegate).getVMArgs()
				: null;
	}

	@Override
	public void setVMArgs(String vmArgs) {
		if (delegate instanceof org.eclipse.jdt.launching.IVMInstall2) {
			((org.eclipse.jdt.launching.IVMInstall2) delegate).setVMArgs(vmArgs);
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public String getJavaVersion() {
		return delegate instanceof org.eclipse.jdt.launching.IVMInstall2
				? ((org.eclipse.jdt.launching.IVMInstall2) delegate).getJavaVersion()
				: null;
	}

	@Override
	public Map<String, String> evaluateSystemProperties(String[] properties,
			com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor monitor) throws CoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IVMInstallRegistry getRegistry() {
		return registry;
	}
}
