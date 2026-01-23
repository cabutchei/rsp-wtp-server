/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.api.dao;

public class ModuleState {
	private DeployableReference deployable;
	private ModuleReference module;
	private int state;
	private int publishState;

	/* required for gson reflective instantiation */
	public ModuleState() {
	}

	public ModuleState(DeployableReference deployable, ModuleReference module, int state, int publishState) {
		this.deployable = deployable;
		this.module = module;
		this.state = state;
		this.publishState = publishState;
	}

	public DeployableReference getDeployable() {
		return deployable;
	}

	public void setDeployable(DeployableReference deployable) {
		this.deployable = deployable;
	}

	public ModuleReference getModule() {
		return module;
	}

	public void setModule(ModuleReference module) {
		this.module = module;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getPublishState() {
		return publishState;
	}

	public void setPublishState(int publishState) {
		this.publishState = publishState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + publishState;
		result = prime * result + ((deployable == null) ? 0 : deployable.hashCode());
		result = prime * result + ((module == null) ? 0 : module.hashCode());
		result = prime * result + state;
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
		ModuleState other = (ModuleState) obj;
		if (publishState != other.publishState)
			return false;
		if (deployable == null) {
			if (other.deployable != null)
				return false;
		} else if (!deployable.equals(other.deployable))
			return false;
		if (module == null) {
			if (other.module != null)
				return false;
		} else if (!module.equals(other.module))
			return false;
		if (state != other.state)
			return false;
		return true;
	}
}
