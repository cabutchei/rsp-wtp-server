/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.foundation.core.launchers;

import com.github.cabutchei.rsp.api.dao.CommandLineDetails;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;

public interface IStartLauncher {
	public ILaunch launch(String mode) throws CoreException;

	public CommandLineDetails getLaunchedDetails();

	public CommandLineDetails getLaunchCommand(String mode) throws CoreException;

	public ILaunch getLaunch();
}
