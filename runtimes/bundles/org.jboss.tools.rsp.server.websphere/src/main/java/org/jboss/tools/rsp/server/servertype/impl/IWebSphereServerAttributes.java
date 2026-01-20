/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.servertype.impl;

import org.jboss.tools.rsp.api.DefaultServerAttributes;

public interface IWebSphereServerAttributes extends DefaultServerAttributes {
	// this property defines what delegate will be bound to server object
	public static final String WEBSPHERE_SERVER_TYPE_PREFIX = "com.ibm.ws.ast.st.v85.server.base";

	/*
	* Other attributes:
		com.ibm.etools.wdt.server.serverType
		com.ibm.ws.ast.st.v7.server.base
		com.ibm.ws.ast.st.v85.server.base
		com.ibm.ws.ast.st.v9.server.base

	*/
	

	/*
	 * Required attributes
	 */
	public static final String SERVER_HOME = DefaultServerAttributes.SERVER_HOME_DIR;
	public static final String WEBSPHERE_PROFILE = "websphere.profile";
	
	/*
	 * Optional attributes
	 */
	public static final String LIBERTY_SERVER_HOST = "liberty.server.host";
	public static final String LIBERTY_SERVER_HOST_DEFAULT = "localhost";
	public static final String LIBERTY_SERVER_PORT = "server.http.port";
	public static final int LIBERTY_SERVER_PORT_DEFAULT = 9080;
}
