package com.github.cabutchei.rsp.server.eap.servertype;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.DefaultServerAttributes;
import com.github.cabutchei.rsp.api.dao.Attributes;
import com.github.cabutchei.rsp.api.dao.ServerLaunchMode;
import com.github.cabutchei.rsp.api.dao.util.CreateServerAttributesUtility;
import com.github.cabutchei.rsp.launching.java.ILaunchModes;
import com.github.cabutchei.rsp.server.eap.impl.EapServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.AbstractServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;

public class EapServerType extends AbstractServerType {
	private Attributes required;
	private Attributes optional;

	public EapServerType(String id, String name, String desc) {
		super(id, name, desc);
	}

	@Override
	public IServerDelegate createServerDelegate(IServer server) {
		if (server == null) {
			return null;
		}
		return new EapServerDelegate(server);
	}

	@Override
	public Attributes getRequiredAttributes() {
		if (required == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(IEapServerAttributes.SERVER_HOME,
					ServerManagementAPIConstants.ATTR_TYPE_LOCAL_FOLDER,
					"A filesystem path pointing to a server installation's root directory",
					null);
			required = attrs.toPojo();
		}
		return required;
	}

	@Override
	public Attributes getOptionalAttributes() {
		if (optional == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(IEapServerAttributes.CONFIG_FILE,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"Standalone configuration file name or absolute path",
					IEapServerAttributes.CONFIG_FILE_DEFAULT);
			attrs.addAttribute(IEapServerAttributes.BASE_DIRECTORY,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"Base directory name under the server home",
					IEapServerAttributes.BASE_DIRECTORY_DEFAULT);
			attrs.addAttribute(IEapServerAttributes.HOSTNAME,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"Hostname to use for server",
					IEapServerAttributes.HOSTNAME_DEFAULT);
			attrs.addAttribute(IEapServerAttributes.WEB_PORT,
					ServerManagementAPIConstants.ATTR_TYPE_INT,
					"Web port to use for server",
					IEapServerAttributes.WEB_PORT_DEFAULT);
			attrs.addAttribute(DefaultServerAttributes.AUTOPUBLISH_ENABLEMENT,
					ServerManagementAPIConstants.ATTR_TYPE_BOOL,
					"Enable the autopublisher.",
					DefaultServerAttributes.AUTOPUBLISH_ENABLEMENT_DEFAULT);
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
