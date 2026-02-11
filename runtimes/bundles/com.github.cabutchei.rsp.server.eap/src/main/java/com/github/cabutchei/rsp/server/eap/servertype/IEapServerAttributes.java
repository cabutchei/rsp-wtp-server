package com.github.cabutchei.rsp.server.eap.servertype;

import com.github.cabutchei.rsp.api.DefaultServerAttributes;

public interface IEapServerAttributes extends DefaultServerAttributes {
	String EAP_70_SERVER_TYPE = "org.jboss.ide.eclipse.as.eap.70";

	String SERVER_HOME = DefaultServerAttributes.SERVER_HOME_DIR;

	String CONFIG_FILE = "org.jboss.ide.eclipse.as.core.server.internal.v7.CONFIG_FILE";
	String CONFIG_FILE_DEFAULT = "standalone.xml";

	String BASE_DIRECTORY = "org.jboss.ide.eclipse.as.core.server.internal.v7.BASE_DIRECTORY";
	String BASE_DIRECTORY_DEFAULT = "standalone";
	
	String HOSTNAME = "hostname";
	String HOSTNAME_DEFAULT = "localhost";

	String WEB_PORT = "org.jboss.ide.eclipse.as.core.server.webPort";
	String WEB_PORT_DEFAULT = "8080";
}
