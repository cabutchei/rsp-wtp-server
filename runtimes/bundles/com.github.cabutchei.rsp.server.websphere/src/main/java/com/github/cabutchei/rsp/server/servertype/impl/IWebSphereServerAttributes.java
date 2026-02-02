package com.github.cabutchei.rsp.server.servertype.impl;

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
