package com.github.cabutchei.rsp.eclipse.wst;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;

/**
 * Typed access to WST-side adapters ("delegates") from RSP-side server objects.
 * <p>
 * Works with both saved servers and unsaved working copies via {@link WstServerAdapterAccess}.
 * </p>
 */
public interface IWstServerDelegateAccess<T> {
	Class<T> getDelegateType();

	default T getDelegate(Object rspServerOrWorkingCopy) throws CoreException {
		return WstServerAdapterAccess.loadAdapter(rspServerOrWorkingCopy, getDelegateType());
	}
}
