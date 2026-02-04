/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.api.dao;

import com.github.cabutchei.rsp.api.dao.util.EqualsUtility;
import com.github.cabutchei.rsp.api.dao.util.Optional;

public class WorkspaceFolder {

	private String uri;

	@Optional
	private String name;

	public WorkspaceFolder() {
	}

	public WorkspaceFolder(String uri, String name) {
		this.uri = uri;
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkspaceFolder other = (WorkspaceFolder) obj;
		return EqualsUtility.areEqual(name, other.name)
				&& EqualsUtility.areEqual(uri, other.uri);
	}
}
