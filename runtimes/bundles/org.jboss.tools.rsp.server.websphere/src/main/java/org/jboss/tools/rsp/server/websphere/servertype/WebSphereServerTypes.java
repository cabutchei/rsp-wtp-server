/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.websphere.servertype;

import org.jboss.tools.rsp.server.servertype.impl.IWebSphereServerAttributes;

public class WebSphereServerTypes {
	public static final String WEBSPHERE_ID = IWebSphereServerAttributes.WEBSPHERE_SERVER_TYPE_PREFIX;
	public static final String WEBSPHERE_NAME = "WebSphere Traditional 8.5.x";
	public static final String WEBSPHERE_DESC =
			"A server adapter capable of discovering and controlling a WebSphere Traditional 8.x runtime instance.";

	public static final WebSphereServerType WEBSPHERE_SERVER_TYPE =
			new WebSphereServerType(WEBSPHERE_ID, WEBSPHERE_NAME, WEBSPHERE_DESC);
}
