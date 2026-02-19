package com.github.cabutchei.rsp.server.liberty.servertype;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.Attributes;
import com.github.cabutchei.rsp.api.dao.ServerLaunchMode;
import com.github.cabutchei.rsp.api.dao.util.CreateServerAttributesUtility;
import com.github.cabutchei.rsp.server.liberty.impl.LibertyServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.AbstractServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;
import com.github.cabutchei.rsp.launching.java.ILaunchModes;

public class LibertyServerType extends AbstractServerType {
	public static final String ATTR_HTTP_PORT = "server.http.port";
	public static final String ATTR_CLASSPATH_ADDITIONS = "server.classpath.additions";

	private Attributes required;
	private Attributes optional;

	public LibertyServerType(String id, String name, String desc) {
		super(id, name, desc);
	}

	@Override
	public IServerDelegate createServerDelegate(IServer server) {
		if (server == null) {
			return null;
		}
		return new LibertyServerDelegate(server);
	}

	@Override
	public Attributes getRequiredAttributes() {
		if (required == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(ILibertyServerAttributes.SERVER_HOME,
					ServerManagementAPIConstants.ATTR_TYPE_LOCAL_FOLDER,
					"A filesystem path pointing to a server installation's root directory",
					null);
			attrs.addAttribute(ILibertyServerAttributes.LIBERTY_PROFILE,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"The server profile name",
					"defaultServer");
			required = attrs.toPojo();
		}
		return required;
	}

	@Override
	public Attributes getOptionalAttributes() {
		if (optional == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(ILibertyServerAttributes.LIBERTY_SERVER_HOST,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"Set the host you want your Liberty instance to bind to.",
					ILibertyServerAttributes.LIBERTY_SERVER_HOST_DEFAULT);
			attrs.addAttribute(ATTR_HTTP_PORT,
					ServerManagementAPIConstants.ATTR_TYPE_INT,
					"HTTP port for the Liberty server",
					9080);
			attrs.addAttribute(ATTR_CLASSPATH_ADDITIONS,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"Additional classpath entries for the Liberty server launch",
					"");
			optional = attrs.toPojo();
		}
		return optional;
	}

	@Override
	public Attributes getRequiredLaunchAttributes() {
		return new CreateServerAttributesUtility().toPojo();
	}

	@Override
	public Attributes getOptionalLaunchAttributes() {
		return new CreateServerAttributesUtility().toPojo();
	}

	@Override
	public ServerLaunchMode[] getLaunchModes() {
		return new ServerLaunchMode[] {
				new ServerLaunchMode(ILaunchModes.RUN, ILaunchModes.RUN_DESC),
				new ServerLaunchMode(ILaunchModes.DEBUG, ILaunchModes.DEBUG_DESC)
		};
	}

}
