/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.api.dao;

import org.jboss.tools.rsp.api.dao.util.EqualsUtility;
import org.jboss.tools.rsp.api.dao.util.Optional;

public class ModuleReference {

	private String id;
	private String name;

	@Optional
	private String typeId;

	public ModuleReference() {
	}

	public ModuleReference(String id, String name, String typeId) {
		this.id = id;
		this.name = name;
		this.typeId = typeId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
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
		ModuleReference other = (ModuleReference) obj;
		return EqualsUtility.areEqual(id, other.id)
				&& EqualsUtility.areEqual(name, other.name)
				&& EqualsUtility.areEqual(typeId, other.typeId);
	}
}
