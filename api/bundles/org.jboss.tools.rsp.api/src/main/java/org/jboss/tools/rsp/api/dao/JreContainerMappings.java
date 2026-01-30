/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.api.dao;

import java.util.ArrayList;
import java.util.List;

public class JreContainerMappings {
	private List<JreContainerMapping> mappings;

	public JreContainerMappings() {
		this.mappings = new ArrayList<>();
	}

	public JreContainerMappings(List<JreContainerMapping> mappings) {
		this.mappings = mappings;
	}

	public List<JreContainerMapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<JreContainerMapping> mappings) {
		this.mappings = mappings;
	}
}
