package com.github.cabutchei.rsp.server.websphere.servertype;

import com.github.cabutchei.rsp.server.servertype.impl.IWebSphereServerAttributes;

public class WebSphereServerTypes {
	public static final String WEBSPHERE_ID = IWebSphereServerAttributes.WEBSPHERE_SERVER_TYPE_PREFIX;
	public static final String WEBSPHERE_NAME = "WebSphere Traditional 8.5.x";
	public static final String WEBSPHERE_DESC =
			"A server adapter capable of discovering and controlling a WebSphere Traditional 8.x runtime instance.";

	public static final WebSphereServerType WEBSPHERE_SERVER_TYPE =
			new WebSphereServerType(WEBSPHERE_ID, WEBSPHERE_NAME, WEBSPHERE_DESC);
}
