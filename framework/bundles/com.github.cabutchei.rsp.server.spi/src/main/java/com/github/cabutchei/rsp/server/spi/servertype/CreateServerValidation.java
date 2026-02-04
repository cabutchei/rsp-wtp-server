/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.servertype;

import java.util.List;

import com.github.cabutchei.rsp.api.dao.CreateServerResponse;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.server.spi.util.StatusConverter;

public class CreateServerValidation {

	private IStatus status;
	private List<String> failedKeys;

	public CreateServerValidation() {
	}

	public CreateServerValidation(IStatus status, List<String> failedKeys) {
		this.status = status;
		this.failedKeys = failedKeys;
		
	}

	public IStatus getStatus() {
		return status;
	}

	public void setStatus(IStatus status) {
		this.status = status;
	}

	public List<String> getFailedKeys() {
		return failedKeys;
	}

	public void setFailedKeys(List<String> failedKeys) {
		this.failedKeys = failedKeys;
	}

	public CreateServerResponse toDao() {
		return new CreateServerResponse(StatusConverter.convert(status), failedKeys);
	}
}
