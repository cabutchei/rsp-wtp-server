package com.github.cabutchei.rsp.server.liberty.servertype;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.Attributes;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.api.dao.ServerLaunchMode;
import org.jboss.tools.rsp.api.dao.ServerType;
import org.jboss.tools.rsp.api.dao.util.CreateServerAttributesUtility;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;
import org.jboss.tools.rsp.server.liberty.custom.impl.Activator;
import org.jboss.tools.rsp.server.liberty.custom.impl.LibertyServerDelegate;
import org.jboss.tools.rsp.server.spi.servertype.AbstractServerType;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;
import org.jboss.tools.rsp.launching.java.ILaunchModes;

public class LibertyServerType extends AbstractServerType {
	public static final String ATTR_LIBERTY_ID = "server.liberty.id";
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
		if (Activator.getWstIntegrationService() == null) {
			return null;
		}
		ServerHandle handle = toHandle(server);
		WSTServerContext context = new WSTServerContext(handle, Activator.getWstIntegrationService().getFacade());
		return new LibertyServerDelegate(server, context);
	}

	@Override
	public Attributes getRequiredAttributes() {
		if (required == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(ILibertyServerAttributes.SERVER_HOME,
					ServerManagementAPIConstants.ATTR_TYPE_LOCAL_FOLDER,
					"A filesystem path pointing to a server installation's root directory",
					null);
			attrs.addAttribute(ATTR_LIBERTY_ID,
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

	private ServerHandle toHandle(IServer server) {
		ServerType type = new ServerType(getId(), getName(), getDescription());
		return new ServerHandle(server.getId(), type);
	}
}
