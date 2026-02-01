/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.liberty.custom.servertype;

import org.jboss.tools.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;

public class LibertyServerTypes {
	public static final String LIBERTY_ID = ILibertyServerAttributes.LIBERTY_SERVER_TYPE_PREFIX;
	public static final String LIBERTY_NAME = "WebSphere Liberty 25.0.x";
	public static final String LIBERTY_DESC = "A server adapter capable of discovering and controlling a WebSphere Liberty 25.x runtime instance.";

	public static final LibertyServerType LIBERTY_SERVER_TYPE =
			new LibertyServerType(LIBERTY_ID, LIBERTY_NAME, LIBERTY_DESC);
}
