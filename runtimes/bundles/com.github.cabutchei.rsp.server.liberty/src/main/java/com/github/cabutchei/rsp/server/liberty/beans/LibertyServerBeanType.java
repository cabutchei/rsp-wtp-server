/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.server.liberty.beans;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.jboss.tools.rsp.server.spi.discovery.ServerBeanType;
import org.jboss.tools.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;

public class LibertyServerBeanType extends ServerBeanType {
	private static final String OPENLIBERTY_PROPS = "lib/versions/openliberty.properties";
	private static final String PRODUCT_ID_KEY = "com.ibm.websphere.productId";
	private static final String PRODUCT_VERSION_KEY = "com.ibm.websphere.productVersion";
	private static final String PRODUCT_ID_PREFIX = "io.openliberty";

	public LibertyServerBeanType() {
		super(ILibertyServerAttributes.LIBERTY_SERVER_TYPE_PREFIX, "WebSphere Liberty");
	}

	@Override
	public boolean isServerRoot(File location) {
		if (location == null) {
			return false;
		}
		File propsFile = new File(location, OPENLIBERTY_PROPS);
		if (!propsFile.isFile()) {
			return false;
		}
		String productId = readProperty(propsFile, PRODUCT_ID_KEY);
		return productId != null && productId.startsWith(PRODUCT_ID_PREFIX);
	}

	@Override
	public String getFullVersion(File root) {
		if (root == null) {
			return null;
		}
		File propsFile = new File(root, OPENLIBERTY_PROPS);
		return readProperty(propsFile, PRODUCT_VERSION_KEY);
	}

	@Override
	public String getUnderlyingTypeId(File root) {
		return getId();
	}

	@Override
	public String getServerAdapterTypeId(String version) {
		return getId();
	}

	private String readProperty(File file, String key) {
		if (file == null || !file.isFile()) {
			return null;
		}
		Properties props = new Properties();
		try (FileInputStream stream = new FileInputStream(file)) {
			props.load(stream);
		} catch (IOException ioe) {
			return null;
		}
		return props.getProperty(key);
	}
}
