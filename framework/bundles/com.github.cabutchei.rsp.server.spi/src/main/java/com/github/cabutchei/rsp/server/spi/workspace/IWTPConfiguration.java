/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.workspace;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;

/**
 * WTP publishing-related configuration operations.
 */
public interface IWTPConfiguration {
	IStatus setGlobalAutoPublishing(boolean enabled);

	IStatus setAutoPublishingForAllServers(boolean enabled);
}
