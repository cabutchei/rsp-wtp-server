package com.github.cabutchei.rsp.eclipse.jdt.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.jdt.adapter.VMInstallAdapter;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstall;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallChangedListener;
import com.github.cabutchei.rsp.eclipse.jdt.launching.IVMInstallRegistry;
import com.github.cabutchei.rsp.eclipse.jdt.launching.PropertyChangeEvent;
import com.github.cabutchei.rsp.eclipse.jdt.launching.VMInstallRegistry;

public class EclipseJdtVMInstallRegistry implements IVMInstallRegistry {
	private final Map<IVMInstallChangedListener, org.eclipse.jdt.launching.IVMInstallChangedListener> listeners = new HashMap<>();

	@Override
	public void addActiveVM() {
		// no-op
	}

	@Override
	public void addVMInstall(IVMInstall vm) throws IllegalArgumentException {
		org.eclipse.jdt.launching.IVMInstall nativeVm = VMInstallAdapter.unwrap(vm);
		if (nativeVm == null || nativeVm.getVMInstallType() == null) {
			throw new IllegalArgumentException();
		}
		persistWorkspaceVmConfiguration();
		org.eclipse.jdt.launching.JavaRuntime.fireVMAdded(nativeVm);
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
			persistWorkspaceVmConfiguration();
			org.eclipse.jdt.launching.JavaRuntime.fireVMRemoved(nativeVm);
		}
	}

	@Override
	public void removeVMInstall(String vmId) {
		if (vmId == null) {
			return;
		}
		for (org.eclipse.jdt.launching.IVMInstallType type : org.eclipse.jdt.launching.JavaRuntime.getVMInstallTypes()) {
			org.eclipse.jdt.launching.IVMInstall nativeVm = type.findVMInstall(vmId);
			if (nativeVm != null) {
				type.disposeVMInstall(vmId);
				persistWorkspaceVmConfiguration();
				org.eclipse.jdt.launching.JavaRuntime.fireVMRemoved(nativeVm);
				return;
			}
		}
	}

	@Override
	public synchronized void addListener(IVMInstallChangedListener l) {
		if (l == null || listeners.containsKey(l)) {
			return;
		}
		org.eclipse.jdt.launching.IVMInstallChangedListener nativeListener = new org.eclipse.jdt.launching.IVMInstallChangedListener() {
			@Override
			public void defaultVMInstallChanged(org.eclipse.jdt.launching.IVMInstall previous,
					org.eclipse.jdt.launching.IVMInstall current) {
				l.defaultVMInstallChanged(VMInstallAdapter.wrap(previous, EclipseJdtVMInstallRegistry.this),
						VMInstallAdapter.wrap(current, EclipseJdtVMInstallRegistry.this));
			}

			@Override
			public void vmChanged(org.eclipse.jdt.launching.PropertyChangeEvent event) {
				l.vmChanged(toRspEvent(event));
			}

			@Override
			public void vmAdded(org.eclipse.jdt.launching.IVMInstall vm) {
				l.vmAdded(VMInstallAdapter.wrap(vm, EclipseJdtVMInstallRegistry.this));
			}

			@Override
			public void vmRemoved(org.eclipse.jdt.launching.IVMInstall vm) {
				l.vmRemoved(VMInstallAdapter.wrap(vm, EclipseJdtVMInstallRegistry.this));
			}
		};
		listeners.put(l, nativeListener);
		org.eclipse.jdt.launching.JavaRuntime.addVMInstallChangedListener(nativeListener);
	}

	@Override
	public synchronized void removeListener(IVMInstallChangedListener l) {
		org.eclipse.jdt.launching.IVMInstallChangedListener nativeListener = listeners.remove(l);
		if (nativeListener != null) {
			org.eclipse.jdt.launching.JavaRuntime.removeVMInstallChangedListener(nativeListener);
		}
	}

	@Override
	public void fireVMChanged(PropertyChangeEvent event) {
		if (event == null) {
			return;
		}
		Object source = event.getSource();
		org.eclipse.jdt.launching.IVMInstall nativeVm = source instanceof IVMInstall
				? VMInstallAdapter.unwrap((IVMInstall) source)
				: null;
		if (nativeVm == null) {
			return;
		}
		org.eclipse.jdt.launching.JavaRuntime.fireVMChanged(new org.eclipse.jdt.launching.PropertyChangeEvent(nativeVm,
				event.getProperty(), unwrapValue(event.getOldValue()), unwrapValue(event.getNewValue())));
	}

	@Override
	public IVMInstall getDefaultVMInstall() {
		return VMInstallAdapter.wrap(org.eclipse.jdt.launching.JavaRuntime.getDefaultVMInstall(), this);
	}

	@Override
	public void load(File f)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException {
		if (f == null || !f.exists()) {
			return;
		}
		VMInstallRegistry legacyRegistry = new VMInstallRegistry();
		legacyRegistry.load(f);
		boolean changed = false;
		for (IVMInstall vm : legacyRegistry.getVMs()) {
			if (vm == null || vm.getInstallLocation() == null) {
				continue;
			}
			if (findVMInstall(vm.getId()) != null || findVMInstall(vm.getInstallLocation()) != null) {
				continue;
			}
			org.eclipse.jdt.launching.IVMInstallType type = findCompatibleType(vm.getInstallLocation());
			if (type == null) {
				continue;
			}
			org.eclipse.jdt.launching.IVMInstall nativeVm = type.createVMInstall(vm.getId());
			nativeVm.setInstallLocation(vm.getInstallLocation());
			nativeVm.setName(vm.getName());
			if (nativeVm instanceof org.eclipse.jdt.launching.IVMInstall2) {
				((org.eclipse.jdt.launching.IVMInstall2) nativeVm).setVMArgs(vm.getVMArgs());
			}
			org.eclipse.jdt.launching.JavaRuntime.fireVMAdded(nativeVm);
			changed = true;
		}
		if (changed) {
			persistWorkspaceVmConfiguration();
		}
	}

	@Override
	public void save(File f) throws IOException {
		try {
			persistWorkspaceVmConfiguration();
		} catch (IllegalArgumentException e) {
			throw new IOException("Failed to persist JDT VM configuration", e);
		}
	}

	private PropertyChangeEvent toRspEvent(org.eclipse.jdt.launching.PropertyChangeEvent event) {
		Object source = event == null ? null : event.getSource();
		Object wrappedSource = source instanceof org.eclipse.jdt.launching.IVMInstall
				? VMInstallAdapter.wrap((org.eclipse.jdt.launching.IVMInstall) source, this)
				: source;
		return new PropertyChangeEvent(wrappedSource, event.getProperty(), wrapValue(event.getOldValue()),
				wrapValue(event.getNewValue()));
	}

	private Object wrapValue(Object value) {
		if (value instanceof org.eclipse.jdt.launching.IVMInstall) {
			return VMInstallAdapter.wrap((org.eclipse.jdt.launching.IVMInstall) value, this);
		}
		return value;
	}

	private Object unwrapValue(Object value) {
		if (value instanceof IVMInstall) {
			return VMInstallAdapter.unwrap((IVMInstall) value);
		}
		return value;
	}

	private void persistWorkspaceVmConfiguration() throws IllegalArgumentException {
		try {
			org.eclipse.jdt.launching.JavaRuntime.saveVMConfiguration();
		} catch (CoreException e) {
			throw new IllegalArgumentException(e);
		}
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

	private String normalizePath(File file) {
		try {
			return file.getCanonicalFile().getAbsolutePath();
		} catch (IOException e) {
			return file.getAbsoluteFile().getAbsolutePath();
		}
	}
}
