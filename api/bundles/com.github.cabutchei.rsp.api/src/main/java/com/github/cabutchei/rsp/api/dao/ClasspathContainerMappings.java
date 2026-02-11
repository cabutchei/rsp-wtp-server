/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package com.github.cabutchei.rsp.api.dao;

import java.util.List;

public class ClasspathContainerMappings {
	private List<ClasspathContainerMapping> mappings;

	public ClasspathContainerMappings() {
	}

	public ClasspathContainerMappings(List<ClasspathContainerMapping> mappings) {
		this.mappings = mappings;
	}

	public List<ClasspathContainerMapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<ClasspathContainerMapping> mappings) {
		this.mappings = mappings;
	}
}
