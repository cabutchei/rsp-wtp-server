/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.api.dao;

public class ClasspathContainerEntry {
	private int entryKind;
	private String path;
	private String sourcePath;
	private String sourceRootPath;
	private String javadocLocation;
	private boolean exported;

	public ClasspathContainerEntry() {
	}

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

	public void setEntryKind(int entryKind) {
		this.entryKind = entryKind;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSourceRootPath() {
		return sourceRootPath;
	}

	public void setSourceRootPath(String sourceRootPath) {
		this.sourceRootPath = sourceRootPath;
	}

	public String getJavadocLocation() {
		return javadocLocation;
	}

	public void setJavadocLocation(String javadocLocation) {
		this.javadocLocation = javadocLocation;
	}

	public boolean isExported() {
		return exported;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}
}
