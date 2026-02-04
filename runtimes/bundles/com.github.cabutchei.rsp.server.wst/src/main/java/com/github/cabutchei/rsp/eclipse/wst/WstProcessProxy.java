package com.github.cabutchei.rsp.eclipse.wst;

import java.util.Objects;

import com.github.cabutchei.rsp.eclipse.debug.core.DebugException;
import com.github.cabutchei.rsp.eclipse.debug.core.DebugPluginConstants;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IProcess;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IStreamsProxy;

public class WstProcessProxy implements IProcess {
	private final org.eclipse.debug.core.model.IProcess wstProcess;
	private final WstLaunchProxy launch;
	private final WstModelAdapter adapter;
	private IStreamsProxy streamsProxy;

	public WstProcessProxy(org.eclipse.debug.core.model.IProcess wstProcess, WstLaunchProxy launch,
			WstModelAdapter adapter) {
		this.wstProcess = Objects.requireNonNull(wstProcess, "wstProcess");
		this.launch = launch;
		// initializeAttributes(attributes);
		this.adapter = adapter == null ? new WstModelAdapter() : adapter;
		this.streamsProxy = new WstStreamsProxy(wstProcess.getStreamsProxy());
		// String captureOutput = launch.getAttribute(DebugPluginConstants.ATTR_CAPTURE_OUTPUT);
		// fCaptureOutput = !("false".equals(captureOutput)); //$NON-NLS-1$
	}

	org.eclipse.debug.core.model.IProcess getWstProcess() {
		return wstProcess;
	}

	@Override
	public String getLabel() {
		return wstProcess.getLabel();
	}


	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	@Override
	public IStreamsProxy getStreamsProxy() {
	    // if (!fCaptureOutput) {
	    //     return null;
	    // }
		return streamsProxy;
	}

	@Override
	public void setAttribute(String key, String value) {
		wstProcess.setAttribute(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return wstProcess.getAttribute(key);
	}

	@Override
	public int getExitValue() throws DebugException {
		try {
			return wstProcess.getExitValue();
		} catch (org.eclipse.debug.core.DebugException e) {
			throw new DebugException(adapter.toRspStatus(e.getStatus()));
		}
	}

	@Override
	public boolean canTerminate() {
		return wstProcess.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return wstProcess.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		try {
			wstProcess.terminate();
		} catch (org.eclipse.debug.core.DebugException e) {
			throw new DebugException(adapter.toRspStatus(e.getStatus()));
		}
	}
}
