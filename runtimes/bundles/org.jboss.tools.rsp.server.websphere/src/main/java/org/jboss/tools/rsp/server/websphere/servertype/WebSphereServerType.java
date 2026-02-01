/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.websphere.servertype;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.Attributes;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.api.dao.ServerLaunchMode;
import org.jboss.tools.rsp.api.dao.ServerType;
import org.jboss.tools.rsp.api.dao.util.CreateServerAttributesUtility;
import org.jboss.tools.rsp.eclipse.wst.WSTServerContext;
import org.jboss.tools.rsp.launching.java.ILaunchModes;
import org.jboss.tools.rsp.server.servertype.impl.IWebSphereServerAttributes;
import org.jboss.tools.rsp.server.spi.servertype.AbstractServerType;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerDelegate;
import org.jboss.tools.rsp.server.websphere.impl.Activator;
import org.jboss.tools.rsp.server.websphere.impl.WebSphereServerDelegate;

public class WebSphereServerType extends AbstractServerType {
	public static final String ATTR_HTTP_PORT = "server.http.port";
	public static final String ATTR_CLASSPATH_ADDITIONS = "server.classpath.additions";

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
		if (Activator.getWstIntegrationService() == null) {
			return null;
		}
		ServerHandle handle = toHandle(server);
		WSTServerContext context = new WSTServerContext(handle, Activator.getWstIntegrationService().getFacade());
		return new WebSphereServerDelegate(server, context);
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
					null);
			required = attrs.toPojo();
		}
		return required;
	}

	@Override
	public Attributes getOptionalAttributes() {
		if (optional == null) {
			CreateServerAttributesUtility attrs = new CreateServerAttributesUtility();
			attrs.addAttribute(ATTR_HTTP_PORT,
					ServerManagementAPIConstants.ATTR_TYPE_INT,
					"HTTP port for the WebSphere server",
					IWebSphereServerAttributes.LIBERTY_SERVER_PORT_DEFAULT);
			attrs.addAttribute(ATTR_CLASSPATH_ADDITIONS,
					ServerManagementAPIConstants.ATTR_TYPE_STRING,
					"Additional classpath entries for the WebSphere server launch",
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
