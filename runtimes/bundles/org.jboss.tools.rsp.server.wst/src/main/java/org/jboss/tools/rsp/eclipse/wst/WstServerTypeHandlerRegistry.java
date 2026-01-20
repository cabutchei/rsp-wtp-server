/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WstServerTypeHandlerRegistry {
	private static final List<WstServerTypeHandler> HANDLERS = new CopyOnWriteArrayList<>();

	private WstServerTypeHandlerRegistry() {
		// static utility
	}

	public static void register(WstServerTypeHandler handler) {
		if( handler != null ) {
			HANDLERS.add(handler);
		}
	}

	public static void unregister(WstServerTypeHandler handler) {
		if( handler != null ) {
			HANDLERS.remove(handler);
		}
	}

	public static WstServerTypeHandler find(String serverTypeId) {
		if( serverTypeId == null ) {
			return null;
		}
		for( WstServerTypeHandler handler : HANDLERS ) {
			if( handler != null && handler.handles(serverTypeId) ) {
				return handler;
			}
		}
		return null;
	}
}
