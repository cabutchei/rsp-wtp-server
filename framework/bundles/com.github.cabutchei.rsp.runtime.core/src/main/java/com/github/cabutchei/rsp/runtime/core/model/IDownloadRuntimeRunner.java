/*************************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package com.github.cabutchei.rsp.runtime.core.model;

import com.github.cabutchei.rsp.api.dao.DownloadSingleRuntimeRequest;
import com.github.cabutchei.rsp.api.dao.WorkflowResponse;

public interface IDownloadRuntimeRunner {

	/**
	 * Begin the workflow of downloading DownloadRuntime objects
	 * based on the properties in the given map. 
	 * 
	 * @param req
	 * @return 
	 */ 
	public WorkflowResponse execute(DownloadSingleRuntimeRequest req);
}
