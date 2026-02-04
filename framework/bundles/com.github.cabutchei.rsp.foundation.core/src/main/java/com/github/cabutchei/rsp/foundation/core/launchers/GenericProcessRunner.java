/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.foundation.core.launchers;

import java.io.IOException;

import com.github.cabutchei.rsp.api.dao.CommandLineDetails;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.debug.core.model.IProcess;
import com.github.cabutchei.rsp.foundation.core.FoundationCoreActivator;

public class GenericProcessRunner {

	private CommandConfig details;

	public GenericProcessRunner(CommandConfig config) {
		this.details = config;
	}

	public CommandLineDetails getCommandLineDetails(ILaunch launch, IProgressMonitor monitor) {
		return getTemporaryDetails().toDetails();
	}
	
	protected CommandConfig getTemporaryDetails() {
		return details;
	}

	public void run(ILaunch launch, IProgressMonitor monitor) throws CoreException {
		runWithDetails(launch, monitor);
	}
	
	public CommandLineDetails runWithDetails(ILaunch launch, IProgressMonitor monitor) throws CoreException {
		CommandConfig det = getTemporaryDetails();
		ProcessUtility util = new ProcessUtility();
		try {
			Process p = util.callProcess(det.getCommand(), det.getParsedArgs(), det.getWorkingDir(), det.getEnvironment());
			IProcess process = util.createIProcess(launch, p, det.toDetails());
			launch.addProcess(process);
			return det.toDetails();
		} catch(IOException ioe) {
			abort("Failed to launch process", ioe, 0);
		}
		return null;
	}

	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, FoundationCoreActivator.PLUGIN_ID, code, message, exception));
	}
	
}
