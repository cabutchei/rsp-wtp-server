package com.github.cabutchei.rsp.eclipse.wst;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.util.IRspAdaptable;

public final class WstServerAdapterAccess {
	private WstServerAdapterAccess() {
		// utility class
	}

	public static org.eclipse.wst.server.core.IServerWorkingCopy toWstServerWorkingCopy(Object rspServerOrWorkingCopy)
			throws CoreException {
		if (rspServerOrWorkingCopy instanceof IRspAdaptable) {
			org.eclipse.wst.server.core.IServerWorkingCopy wc =
					((IRspAdaptable) rspServerOrWorkingCopy).getAdapter(org.eclipse.wst.server.core.IServerWorkingCopy.class);
			if (wc != null) {
				return wc;
			}
		}
		throw unsupported("WST server working copy");
	}

	public static org.eclipse.wst.server.core.IServer toWstServer(Object rspServerOrWorkingCopy) throws CoreException {
		if (rspServerOrWorkingCopy instanceof IRspAdaptable) {
			org.eclipse.wst.server.core.IServer server =
					((IRspAdaptable) rspServerOrWorkingCopy).getAdapter(org.eclipse.wst.server.core.IServer.class);
			if (server != null) {
				return server;
			}
		}
		if (rspServerOrWorkingCopy instanceof IWstServerBacked) {
			return ((IWstServerBacked) rspServerOrWorkingCopy).getWstServer();
		}
		throw unsupported("WST server");
	}

	public static <T> T loadAdapter(Object rspServerOrWorkingCopy, Class<T> adapterType) throws CoreException {
		if (adapterType == null) {
			throw new IllegalArgumentException("adapterType is required");
		}

		if (rspServerOrWorkingCopy instanceof IRspAdaptable) {
			T adapted = ((IRspAdaptable) rspServerOrWorkingCopy).getAdapter(adapterType);
			if (adapted != null) {
				return adapted;
			}
		}

		if (rspServerOrWorkingCopy instanceof IWstServerBacked) {
			org.eclipse.wst.server.core.IServer server = toWstServer(rspServerOrWorkingCopy);
			return adapterType.cast(server.loadAdapter(adapterType, null));
		}

		throw unsupported("WST adapter " + adapterType.getName());
	}

	private static CoreException unsupported(String what) {
		IStatus status = new Status(IStatus.ERROR, "com.github.cabutchei.rsp.server.wst",
				"Object does not provide access to " + what);
		return new CoreException(status);
	}
}
