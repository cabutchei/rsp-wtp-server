package com.github.cabutchei.rsp.eclipse.jdt.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.cabutchei.rsp.eclipse.jdt.adapter.VMInstallAdapter;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallChangedListener;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallRegistry;
import com.github.cabutchei.rsp.eclipse.jdt.launching.PropertyChangeEvent;

public class EclipseJdtVMInstallRegistry implements IVMInstallRegistry {
	@Override
	public void addActiveVM() {
		// no-op
	}

	@Override
	public void addVMInstall(IVMInstall vm) throws IllegalArgumentException {
		// Native JDT VM install types manage registration. Nothing to do here.
	}

	@Override
	public IVMInstall[] getVMs() {
		List<IVMInstall> ret = new ArrayList<>();
		for (org.eclipse.jdt.launching.IVMInstallType type : org.eclipse.jdt.launching.JavaRuntime.getVMInstallTypes()) {
			if (type == null) {
				continue;
			}
			for (org.eclipse.jdt.launching.IVMInstall vm : type.getVMInstalls()) {
				ret.add(VMInstallAdapter.wrap(vm, this));
			}
		}
		return ret.toArray(new IVMInstall[0]);
	}

	@Override
	public IVMInstall findVMInstall(String id) {
		if (id == null) {
			return null;
		}
		for (org.eclipse.jdt.launching.IVMInstallType type : org.eclipse.jdt.launching.JavaRuntime.getVMInstallTypes()) {
			if (type == null) {
				continue;
			}
			org.eclipse.jdt.launching.IVMInstall vm = type.findVMInstall(id);
			if (vm != null) {
				return VMInstallAdapter.wrap(vm, this);
			}
		}
		return null;
	}

	@Override
	public IVMInstall findVMInstall(File installLocation) {
		if (installLocation == null) {
			return null;
		}
		String expected = normalizePath(installLocation);
		for (IVMInstall vm : getVMs()) {
			File loc = vm.getInstallLocation();
			if (loc != null && expected.equals(normalizePath(loc))) {
				return vm;
			}
		}
		return null;
	}

	@Override
	public void removeVMInstall(IVMInstall vm) {
		org.eclipse.jdt.launching.IVMInstall nativeVm = VMInstallAdapter.unwrap(vm);
		if (nativeVm != null && nativeVm.getVMInstallType() != null) {
			nativeVm.getVMInstallType().disposeVMInstall(nativeVm.getId());
		}
	}

	@Override
	public void removeVMInstall(String vmId) {
		if (vmId == null) {
			return;
		}
		for (org.eclipse.jdt.launching.IVMInstallType type : org.eclipse.jdt.launching.JavaRuntime.getVMInstallTypes()) {
			if (type.findVMInstall(vmId) != null) {
				type.disposeVMInstall(vmId);
				return;
			}
		}
	}

	@Override
	public void addListener(IVMInstallChangedListener l) {
		// no-op for now
	}

	@Override
	public void removeListener(IVMInstallChangedListener l) {
		// no-op for now
	}

	@Override
	public void fireVMChanged(PropertyChangeEvent event) {
		// no-op for now
	}

	@Override
	public IVMInstall getDefaultVMInstall() {
		return VMInstallAdapter.wrap(org.eclipse.jdt.launching.JavaRuntime.getDefaultVMInstall(), this);
	}

	@Override
	public void load(File f)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException {
		// no-op
	}

	@Override
	public void save(File f) throws IOException {
		// no-op
	}

	private String normalizePath(File file) {
		try {
			return file.getCanonicalFile().getAbsolutePath();
		} catch (IOException e) {
			return file.getAbsoluteFile().getAbsolutePath();
		}
	}
}
