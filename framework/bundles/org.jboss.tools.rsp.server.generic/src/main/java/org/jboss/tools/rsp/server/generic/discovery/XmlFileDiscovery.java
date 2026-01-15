/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.generic.discovery;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.rsp.server.generic.discovery.internal.XmlUtility;
import org.jboss.tools.rsp.server.generic.matchers.GlobScanner;
import org.jboss.tools.rsp.server.spi.discovery.ServerBeanType;

public class XmlFileDiscovery extends ServerBeanType {
	protected String nameFileString;
	protected String nameKey;
	protected String requiredNamePrefix;
	protected String versionFileString;
	protected String versionKey;
	protected String requiredVersionPrefix;
	protected String serverAdapterTypeId;
	protected boolean nameFileStringIsPattern;
	protected boolean versionFileStringIsPattern;

	public XmlFileDiscovery(String id, String name, String serverAdapterTypeId, String nameFileString,
			boolean nameFileStringIsPattern, String nameKey, String requiredNamePrefix, String versionFileString,
			boolean versionFileStringIsPattern, String versionKey, String requiredVersionPrefix) {
		super(id, name);
		this.serverAdapterTypeId = serverAdapterTypeId;
		this.nameFileString = nameFileString;
		this.nameFileStringIsPattern = nameFileStringIsPattern;
		this.nameKey = nameKey;
		this.requiredNamePrefix = requiredNamePrefix;
		this.versionFileStringIsPattern = versionFileStringIsPattern;
		this.versionFileString = versionFileString;
		this.versionKey = versionKey;
		this.requiredVersionPrefix = requiredVersionPrefix;
	}

	@Override
	public boolean isServerRoot(File location) {
		if (nameKey != null && nameFileString != null) {
			String serverName = getFullName(location);
			if (serverName == null || !matchesPrefix(serverName, requiredNamePrefix)) {
				return false;
			}
		}
		if (versionKey != null && versionFileString != null) {
			String version = getFullVersion(location);
			if (version == null || !matchesPrefix(version, requiredVersionPrefix)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getFullVersion(File root) {
		if (root == null || versionKey == null || versionFileString == null) {
			return null;
		}
		if (!versionFileStringIsPattern) {
			return getProperty(new File(root, versionFileString), versionKey);
		}
		List<String> includes = Arrays.asList(new String[] { versionFileString });
		GlobScanner scanner = new GlobScanner(root, includes, Collections.emptyList(), true);
		List<String> results = scanner.matches();
		if (results != null) {
			for (String path : results) {
				String value = getProperty(new File(root, path), versionKey);
				if (value != null) {
					return value;
				}
			}
		}
		return null;
	}

	public String getFullName(File root) {
		if (root == null || nameKey == null || nameFileString == null) {
			return null;
		}
		if (!nameFileStringIsPattern) {
			return getProperty(new File(root, nameFileString), nameKey);
		}
		List<String> includes = Arrays.asList(new String[] { nameFileString });
		GlobScanner scanner = new GlobScanner(root, includes, Collections.emptyList(), true);
		List<String> results = scanner.matches();
		if (results != null) {
			for (String path : results) {
				String value = getProperty(new File(root, path), nameKey);
				if (value != null) {
					return value;
				}
			}
		}
		return null;
	}

	@Override
	public String getUnderlyingTypeId(File root) {
		return getId();
	}

	protected String getProperty(File f, String key) {
		return XmlUtility.getValue(f, key);
	}

	@Override
	public String getServerAdapterTypeId(String version) {
		return serverAdapterTypeId;
	}

	private boolean matchesPrefix(String value, String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return true;
		}
		return value != null && value.startsWith(prefix);
	}
}
