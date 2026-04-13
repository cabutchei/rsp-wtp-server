package com.github.cabutchei.rsp.server.websphere.servertype;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.DefaultServerAttributes;
import com.github.cabutchei.rsp.api.dao.Attributes;
import com.github.cabutchei.rsp.api.dao.ServerLaunchMode;
import com.github.cabutchei.rsp.api.dao.util.CreateServerAttributesUtility;
import com.github.cabutchei.rsp.launching.java.ILaunchModes;
import com.github.cabutchei.rsp.server.spi.servertype.AbstractServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;

import com.github.cabutchei.rsp.server.servertype.impl.IWebSphereServerAttributes;
import com.github.cabutchei.rsp.server.websphere.impl.WebSphereServerDelegate;

public class WebSphereServerType extends AbstractServerType {

	private Attributes required;
	private Attributes optional;

	public WebSphereServerType(String id, String name, String desc) {
		super(id, name, desc);
	}

	@Override
	public IServerDelegate createServerDelegate(IServer server) {
		if (server == null) {
			return null;
		}
		return new WebSphereServerDelegate(server);
	}

	@Override
	public Attributes getRequiredAttributes() {
		if (required == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(IWebSphereServerAttributes.SERVER_HOME,
					ServerManagementAPIConstants.ATTR_TYPE_LOCAL_FOLDER,
					"A filesystem path pointing to a server installation's root directory",
					null);
			attrs.addAttribute(IWebSphereServerAttributes.WEBSPHERE_PROFILE,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"The server profile name",
					"AppSrv01");
			required = attrs.toPojo();
		}
		return required;
	}

	@Override
	public Attributes getOptionalAttributes() {
		if (optional == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(DefaultServerAttributes.AUTOPUBLISH_ENABLEMENT,
					ServerManagementAPIConstants.ATTR_TYPE_BOOL,
					"Enable the autopublisher.",
					false);
			attrs.addAttribute(DefaultServerAttributes.AUTOPUBLISH_INACTIVITY_LIMIT,
					ServerManagementAPIConstants.ATTR_TYPE_INT,
					"Set the inactivity limit before the autopublisher runs.",
					DefaultServerAttributes.AUTOPUBLISH_INACTIVITY_LIMIT_DEFAULT);
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
