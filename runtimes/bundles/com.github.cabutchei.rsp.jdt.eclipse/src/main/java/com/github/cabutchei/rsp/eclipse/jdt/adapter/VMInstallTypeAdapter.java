package com.github.cabutchei.rsp.eclipse.jdt.adapter;

import java.io.File;

import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallType;

final class VMInstallTypeAdapter implements IVMInstallType {
	private final org.eclipse.jdt.launching.IVMInstallType delegate;

	VMInstallTypeAdapter(org.eclipse.jdt.launching.IVMInstallType delegate) {
		this.delegate = delegate;
	}

	org.eclipse.jdt.launching.IVMInstallType getDelegate() {
		return delegate;
	}

	@Override
	public IVMInstall createVMInstall(String id) {
		throw new UnsupportedOperationException("Use JdtVMService to create VM installs");
	}

	@Override
	public String getName() {
		return delegate == null ? null : delegate.getName();
	}

	@Override
	public String getId() {
		return delegate == null ? null : delegate.getId();
	}

	@Override
	public com.github.cabutchei.rsp.eclipse.core.runtime.IStatus validateInstallLocation(File installLocation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public File detectInstallLocation() {
		return delegate == null ? null : delegate.detectInstallLocation();
	}

	@Override
	public com.github.cabutchei.rsp.eclipse.jdt.launching.LibraryLocation[] getDefaultLibraryLocations(File installLocation) {
		return null;
	}
}
