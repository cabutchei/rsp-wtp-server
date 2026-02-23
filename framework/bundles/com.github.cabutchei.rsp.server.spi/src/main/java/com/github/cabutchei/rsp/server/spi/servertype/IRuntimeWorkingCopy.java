package com.github.cabutchei.rsp.server.spi.servertype;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;

/**
 * RSP-side wrapper around a WST {@code IRuntimeWorkingCopy}.
 */
public interface IRuntimeWorkingCopy extends IRuntime {
	public IRuntime save(boolean force) throws CoreException;
}
