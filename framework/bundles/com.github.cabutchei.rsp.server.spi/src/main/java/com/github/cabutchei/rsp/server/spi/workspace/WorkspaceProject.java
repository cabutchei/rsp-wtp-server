/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import java.nio.file.Path;

public class WorkspaceProject {
	private final String name;
	private final Path location;
	private final boolean open;

	public WorkspaceProject(String name, Path location, boolean open) {
		this.name = name;
		this.location = location;
		this.open = open;
	}

	public String getName() {
		return name;
	}

	public Path getLocation() {
		return location;
	}

	public boolean isOpen() {
		return open;
	}
}
