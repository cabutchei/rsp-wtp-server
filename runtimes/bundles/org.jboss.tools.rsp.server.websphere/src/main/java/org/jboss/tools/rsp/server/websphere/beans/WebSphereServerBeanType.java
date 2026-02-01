/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.websphere.beans;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.rsp.server.servertype.impl.IWebSphereServerAttributes;
import org.jboss.tools.rsp.server.spi.discovery.ServerBeanType;

public class WebSphereServerBeanType extends ServerBeanType {
	private static final String PLATFORM_WEBSHERE_PATH = "properties/version/platform.websphere";
	private static final String NAME_REQUIRED_PREFIX = "IBM WebSphere";
	private static final String VERSION_REQUIRED_PREFIX = "8.5";
private static final Pattern WEBSHERE_TAG_PATTERN = Pattern.compile("<websphere\\b[^>]*>", Pattern.CASE_INSENSITIVE);
private static final Pattern NAME_XML_PATTERN = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
private static final Pattern VERSION_XML_PATTERN = Pattern.compile("version\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	private static final Pattern NAME_PROP_PATTERN = Pattern.compile("(?m)^\\s*name\\s*=\\s*([^\\r\\n]+)$");
	private static final Pattern VERSION_PROP_PATTERN = Pattern.compile("(?m)^\\s*version\\s*=\\s*([^\\r\\n]+)$");

	public WebSphereServerBeanType() {
		super(IWebSphereServerAttributes.WEBSPHERE_SERVER_TYPE_PREFIX, "WebSphere Traditional");
	}

	@Override
	public boolean isServerRoot(File location) {
		if (location == null) {
			return false;
		}
		File descriptor = new File(location, PLATFORM_WEBSHERE_PATH);
		if (!descriptor.isFile()) {
			return false;
		}
		String contents = readContents(descriptor.toPath());
		if (contents == null) {
			return false;
		}
		String name = findXmlValue(contents, NAME_XML_PATTERN);
		String version = findXmlValue(contents, VERSION_XML_PATTERN);
		if (name == null || version == null) {
			name = findPropValue(contents, NAME_PROP_PATTERN);
			version = findPropValue(contents, VERSION_PROP_PATTERN);
		}
		if (name == null || version == null) {
			return false;
		}
		return name.startsWith(NAME_REQUIRED_PREFIX) && version.startsWith(VERSION_REQUIRED_PREFIX);
	}

	@Override
	public String getFullVersion(File root) {
		if (root == null) {
			return null;
		}
		File descriptor = new File(root, PLATFORM_WEBSHERE_PATH);
		String contents = readContents(descriptor.toPath());
		if (contents == null) {
			return null;
		}
		String version = findXmlValue(contents, VERSION_XML_PATTERN);
		if (version != null) {
			return version;
		}
		return findPropValue(contents, VERSION_PROP_PATTERN);
	}

	@Override
	public String getUnderlyingTypeId(File root) {
		return getId();
	}

	@Override
	public String getServerAdapterTypeId(String version) {
		return getId();
	}

	private String readContents(Path path) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
	}

	private String findXmlValue(String contents, Pattern attrPattern) {
		if (contents == null) {
			return null;
		}
		Matcher tagMatcher = WEBSHERE_TAG_PATTERN.matcher(contents);
		if (!tagMatcher.find()) {
			return null;
		}
		String tag = tagMatcher.group(0);
		Matcher matcher = attrPattern.matcher(tag);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return null;
	}

	private String findPropValue(String contents, Pattern propPattern) {
		if (contents == null) {
			return null;
		}
		Matcher matcher = propPattern.matcher(contents);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return null;
	}
}
