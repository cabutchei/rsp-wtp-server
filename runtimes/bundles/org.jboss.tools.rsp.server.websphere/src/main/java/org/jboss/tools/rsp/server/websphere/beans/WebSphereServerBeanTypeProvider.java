/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.websphere.beans;

import org.jboss.tools.rsp.server.spi.discovery.IServerBeanTypeProvider;
import org.jboss.tools.rsp.server.spi.discovery.ServerBeanType;

public class WebSphereServerBeanTypeProvider implements IServerBeanTypeProvider {
	private static final ServerBeanType[] TYPES = new ServerBeanType[] {
			new WebSphereServerBeanType()
	};

	@Override
	public ServerBeanType[] getServerBeanTypes() {
		return TYPES;
	}
}
