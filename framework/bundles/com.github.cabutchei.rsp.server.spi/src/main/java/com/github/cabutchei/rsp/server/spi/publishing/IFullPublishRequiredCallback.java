/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.publishing;

import com.github.cabutchei.rsp.server.spi.filewatcher.FileWatcherEvent;

public interface IFullPublishRequiredCallback {

	public boolean requiresFullPublish(FileWatcherEvent event);

}
