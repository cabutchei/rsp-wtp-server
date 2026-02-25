package com.github.cabutchei.rsp.eclipse.wst.api;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;

public interface WstServerTypeHandler {
	boolean handles(String serverTypeId);

	// TODO: eliminate this from the contract
	void configureServer(IServerWorkingCopy server,
			IRuntimeWorkingCopy runtime,
			Map<String, Object> attributes,
			IProgressMonitor monitor) throws CoreException;
	
	default void configureServer(com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy server,
			Map<String, Object> attributes) throws CoreException {}
}
