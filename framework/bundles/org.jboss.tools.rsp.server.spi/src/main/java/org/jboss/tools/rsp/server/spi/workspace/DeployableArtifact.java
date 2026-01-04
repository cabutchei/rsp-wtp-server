/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.spi.workspace;

import java.nio.file.Path;

public class DeployableArtifact {
	private final String projectName;
	private final String label;
	private final Path deployPath;
	private final String moduleTypeId;

	public DeployableArtifact(String projectName, String label, Path deployPath, String moduleTypeId) {
		this.projectName = projectName;
		this.label = label;
		this.deployPath = deployPath;
		this.moduleTypeId = moduleTypeId;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getLabel() {
		return label;
	}

	public Path getDeployPath() {
		return deployPath;
	}

	public String getModuleTypeId() {
		return moduleTypeId;
	}
}
