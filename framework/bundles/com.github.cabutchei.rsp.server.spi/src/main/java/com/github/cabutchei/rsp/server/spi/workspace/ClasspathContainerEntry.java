/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.server.spi.workspace;

public class ClasspathContainerEntry {
	private final int entryKind;
	private final String path;
	private final String sourcePath;
	private final String sourceRootPath;
	private final String javadocLocation;
	private final boolean exported;

	public ClasspathContainerEntry(int entryKind, String path, String sourcePath, String sourceRootPath,
			String javadocLocation, boolean exported) {
		this.entryKind = entryKind;
		this.path = path;
		this.sourcePath = sourcePath;
		this.sourceRootPath = sourceRootPath;
		this.javadocLocation = javadocLocation;
		this.exported = exported;
	}

	public int getEntryKind() {
		return entryKind;
	}

	public String getPath() {
		return path;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public String getSourceRootPath() {
		return sourceRootPath;
	}

	public String getJavadocLocation() {
		return javadocLocation;
	}

	public boolean isExported() {
		return exported;
	}
}
