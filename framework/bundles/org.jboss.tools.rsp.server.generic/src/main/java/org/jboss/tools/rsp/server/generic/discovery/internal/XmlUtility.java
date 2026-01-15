/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.generic.discovery.internal;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlUtility {

	private XmlUtility() {
		// prevent instantiation
	}

	public static String getValue(File xmlFile, String key) {
		if (xmlFile == null || key == null || key.trim().isEmpty()) {
			return null;
		}
		Document doc = parse(xmlFile);
		if (doc == null) {
			return null;
		}
		ParsedKey parsed = ParsedKey.parse(key);
		if (parsed.attributeName != null) {
			return getAttribute(doc, parsed.tagName, parsed.attributeName);
		}
		String value = getAttribute(doc, null, key);
		if (value != null) {
			return value;
		}
		return getElementText(doc, key);
	}

	public static String getAttribute(File xmlFile, String tagName, String attributeName) {
		if (xmlFile == null || attributeName == null || attributeName.trim().isEmpty()) {
			return null;
		}
		Document doc = parse(xmlFile);
		return getAttribute(doc, tagName, attributeName);
	}

	public static String getElementText(File xmlFile, String tagName) {
		if (xmlFile == null || tagName == null || tagName.trim().isEmpty()) {
			return null;
		}
		Document doc = parse(xmlFile);
		return getElementText(doc, tagName);
	}

	private static String getAttribute(Document doc, String tagName, String attributeName) {
		Element element = findElement(doc, tagName);
		if (element == null || attributeName == null || attributeName.trim().isEmpty()) {
			return null;
		}
		if (!element.hasAttribute(attributeName)) {
			return null;
		}
		return normalize(element.getAttribute(attributeName));
	}

	private static String getElementText(Document doc, String tagName) {
		Element element = findElement(doc, tagName);
		if (element == null) {
			return null;
		}
		return normalize(element.getTextContent());
	}

	private static Element findElement(Document doc, String tagName) {
		if (doc == null) {
			return null;
		}
		if (tagName == null || tagName.trim().isEmpty()) {
			return doc.getDocumentElement();
		}
		NodeList nodes = doc.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return null;
		}
		Node node = nodes.item(0);
		return node instanceof Element ? (Element) node : null;
	}

	private static Document parse(File file) {
		if (file == null || !file.isFile()) {
			return null;
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		try {
			configureFactory(factory);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(file);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			return null;
		}
	}

	private static void configureFactory(DocumentBuilderFactory factory) throws ParserConfigurationException {
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		try {
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		} catch (IllegalArgumentException e) {
			// ignore implementations that do not support the attributes
		}
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static final class ParsedKey {
		private final String tagName;
		private final String attributeName;

		private ParsedKey(String tagName, String attributeName) {
			this.tagName = normalize(tagName);
			this.attributeName = normalize(attributeName);
		}

		private static ParsedKey parse(String key) {
			String trimmed = normalize(key);
			if (trimmed == null) {
				return new ParsedKey(null, null);
			}
			int at = trimmed.indexOf('@');
			if (at < 0) {
				return new ParsedKey(null, null);
			}
			String tag = at == 0 ? null : trimmed.substring(0, at);
			String attribute = trimmed.substring(at + 1);
			return new ParsedKey(tag, attribute);
		}
	}
}
