/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server;

final class OsgiClassLoaderHolder {
	private static volatile ClassLoader classLoader;

	private OsgiClassLoaderHolder() {
	}

	static void set(ClassLoader loader) {
		if (loader != null) {
			classLoader = loader;
		}
	}

	static ClassLoader get() {
		return classLoader;
	}
}
