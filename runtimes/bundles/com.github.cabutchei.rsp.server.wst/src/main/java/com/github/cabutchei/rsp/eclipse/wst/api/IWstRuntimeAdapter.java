package com.github.cabutchei.rsp.eclipse.wst.api;

import com.github.cabutchei.rsp.server.spi.util.IRspAdaptable;

/**
 * Lightweight RSP-side wrapper around a WST {@code IRuntime}/{@code IRuntimeWorkingCopy}.
 */
public interface IWstRuntimeAdapter extends IRspAdaptable {
	String getId();

	String getName();

	default <T> Object loadAdapter(Class<T> adapterType) {
		return null;
	}
}
