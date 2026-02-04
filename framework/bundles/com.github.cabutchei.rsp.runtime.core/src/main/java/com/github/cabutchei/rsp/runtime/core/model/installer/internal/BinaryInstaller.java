/*************************************************************************************
 * Copyright (c) 2018-2019 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package com.github.cabutchei.rsp.runtime.core.model.installer.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.core.runtime.SubProgressMonitor;
import com.github.cabutchei.rsp.foundation.core.tasks.TaskModel;
import com.github.cabutchei.rsp.runtime.core.RuntimeCoreActivator;
import com.github.cabutchei.rsp.runtime.core.model.DownloadRuntime;
import com.github.cabutchei.rsp.runtime.core.model.IDownloadRuntimeWorkflowConstants;
import com.github.cabutchei.rsp.runtime.core.model.IDownloadRuntimesModel;
import com.github.cabutchei.rsp.runtime.core.model.IRuntimeInstaller;
import com.github.cabutchei.rsp.runtime.core.util.internal.DownloadRuntimeOperationUtility;

public class BinaryInstaller implements IRuntimeInstaller {

	private IDownloadRuntimesModel downloadRuntimesModel;
	public BinaryInstaller(IDownloadRuntimesModel downloadRuntimesModel) {
		this.downloadRuntimesModel = downloadRuntimesModel;
	}

	@Override
	public IStatus installRuntime(DownloadRuntime downloadRuntime, String unzipDirectory, String downloadDirectory,
			boolean deleteOnExit, TaskModel taskModel, IProgressMonitor monitor) {
		String user = (String)taskModel.getObject(IDownloadRuntimeWorkflowConstants.USERNAME_KEY);
		String pass = (String)taskModel.getObject(IDownloadRuntimeWorkflowConstants.PASSWORD_KEY);
		
		monitor.beginTask("Install Runtime '" + downloadRuntime.getName() + "' ...", 100);//$NON-NLS-1$ //$NON-NLS-2$
		monitor.worked(1);
		try {
			DownloadRuntimeOperationUtility opUtil = DownloadRuntimeOperationUtilFactory
					.createDownloadRuntimeOperationUtility(taskModel, downloadRuntimesModel);
			File f = opUtil.download(unzipDirectory, downloadDirectory, 
					getDownloadUrl(downloadRuntime, taskModel), deleteOnExit, user, pass, new SubProgressMonitor(monitor, 80));
			File dest = new File(unzipDirectory, f.getName());
			boolean renamed = f.renameTo(dest);
			if( !renamed ) {
				copy(f, dest);
			}
			if (!dest.setExecutable(true)) {
				throw new CoreException(new Status(IStatus.ERROR, RuntimeCoreActivator.PLUGIN_ID, "Can't set executable bit to " + dest.getAbsolutePath()));
			}
			taskModel.putObject(IDownloadRuntimeWorkflowConstants.UNZIPPED_SERVER_HOME_DIRECTORY, unzipDirectory);
			taskModel.putObject(IDownloadRuntimeWorkflowConstants.UNZIPPED_SERVER_BIN, dest.getAbsolutePath());
		} catch(CoreException ce) {
			return ce.getStatus();
		}
		return Status.OK_STATUS;
	}

	private void copy(File f, File dest) throws CoreException {
		try {
			Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch(IOException ioe) {
			throw new CoreException(new Status(IStatus.ERROR, RuntimeCoreActivator.PLUGIN_ID, ioe.getMessage(), ioe));
		}
	}

	private String getDownloadUrl(DownloadRuntime downloadRuntime, TaskModel taskModel) {
		if (downloadRuntime != null) {
			String dlUrl = downloadRuntime.getUrl();
			if (dlUrl == null) {
				return (String) taskModel.getObject(IDownloadRuntimeWorkflowConstants.DL_RUNTIME_URL);
			}
			return dlUrl;
		}
		return null;
	}

}
