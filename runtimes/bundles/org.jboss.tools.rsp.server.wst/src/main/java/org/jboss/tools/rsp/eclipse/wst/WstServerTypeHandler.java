/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.eclipse.wst;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;

public interface WstServerTypeHandler {
	boolean handles(String serverTypeId);

	void configureServer(IServerWorkingCopy server,
			IRuntimeWorkingCopy runtime,
			Map<String, Object> attributes,
			IProgressMonitor monitor) throws CoreException;
}
