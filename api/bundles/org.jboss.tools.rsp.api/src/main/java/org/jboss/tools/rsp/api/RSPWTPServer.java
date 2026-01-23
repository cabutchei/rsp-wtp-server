/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.api;

/**
 * Combined server interface that exposes both RSPServer and WTPServer methods
 * over a single JSON-RPC connection.
 */
public interface RSPWTPServer extends RSPServer, WTPServer {
	// marker interface
}
