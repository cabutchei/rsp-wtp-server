package com.github.cabutchei.rsp.eclipse.wst.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.debug.core.DebugException;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IProcess;
import com.github.cabutchei.rsp.eclipse.wst.adapter.WstModelAdapter;



public class WstLaunchProxy implements ILaunch {
	private final org.eclipse.debug.core.ILaunch wstLaunch;

	public WstLaunchProxy(org.eclipse.debug.core.ILaunch wstLaunch) {
		this.wstLaunch = Objects.requireNonNull(wstLaunch, "wstLaunch");
	}

	org.eclipse.debug.core.ILaunch getWstLaunch() {
		return wstLaunch;
	}

	@Override
	public Object[] getChildren() {
		Object[] children = wstLaunch.getChildren();
		if (children == null || children.length == 0) {
			return new Object[0];
		}
		Object[] wrapped = new Object[children.length];
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof org.eclipse.debug.core.model.IProcess) {
				wrapped[i] = new WstProcessProxy((org.eclipse.debug.core.model.IProcess) child, this);
			} else {
				wrapped[i] = child;
			}
		}
		return wrapped;
	}

	@Override
	public IProcess[] getProcesses() {
		org.eclipse.debug.core.model.IProcess[] processes = wstLaunch.getProcesses();
		if (processes == null || processes.length == 0) {
			return new IProcess[0];
		}
		List<IProcess> result = new ArrayList<>(processes.length);
		for (org.eclipse.debug.core.model.IProcess process : processes) {
			result.add(new WstProcessProxy(process, this));
		}
		return result.toArray(new IProcess[result.size()]);
	}

	@Override
	public void addProcess(IProcess process) {
		if (process instanceof WstProcessProxy) {
			wstLaunch.addProcess(((WstProcessProxy) process).getWstProcess());
		}
	}

	@Override
	public void removeProcess(IProcess process) {
		if (process instanceof WstProcessProxy) {
			wstLaunch.removeProcess(((WstProcessProxy) process).getWstProcess());
		}
	}

	@Override
	public String getLaunchMode() {
		return wstLaunch.getLaunchMode();
	}

	@Override
	public void setAttribute(String key, String value) {
		wstLaunch.setAttribute(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return wstLaunch.getAttribute(key);
	}

	@Override
	public boolean hasChildren() {
		return wstLaunch.hasChildren();
	}

	@Override
	public boolean canTerminate() {
		return wstLaunch.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return wstLaunch.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		try {
			wstLaunch.terminate();
		} catch (org.eclipse.debug.core.DebugException e) {
			throw new DebugException(WstModelAdapter.toRspStatus(e.getStatus()));
		}
	}
}
