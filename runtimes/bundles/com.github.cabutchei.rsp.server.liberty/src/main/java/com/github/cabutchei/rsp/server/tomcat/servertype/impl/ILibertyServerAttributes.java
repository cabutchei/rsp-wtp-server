package com.github.cabutchei.rsp.server.liberty.tomcat.servertype.impl;

import org.jboss.tools.rsp.api.DefaultServerAttributes;

public interface ILibertyServerAttributes extends DefaultServerAttributes {
	// this property defines what delegate will be bound to server object
	public static final String LIBERTY_SERVER_TYPE_PREFIX = "com.ibm.ws.st.server.wlp";
	

	/*
	 * Required attributes
	 */
	public static final String SERVER_HOME = DefaultServerAttributes.SERVER_HOME_DIR;
	
	/*
	 * Optional attributes
	 */
	public static final String LIBERTY_SERVER_HOST = "liberty.server.host";
	public static final String LIBERTY_SERVER_HOST_DEFAULT = "localhost";
	public static final String LIBERTY_SERVER_PORT = "server.http.port";
	public static final int LIBERTY_SERVER_PORT_DEFAULT = 9080;
}
