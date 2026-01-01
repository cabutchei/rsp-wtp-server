/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.spi.model;

import java.util.Collections;
import java.util.Map;

import org.jboss.tools.rsp.eclipse.core.runtime.CoreException;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.spi.servertype.CreateServerValidation;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerType;

/**
 * Hook points for integrating external lifecycle behavior into the server model.
 * Default implementations are no-ops to keep the strategy optional.
 */
public interface ServerLifecycleStrategy {

	/**
	 * Validate a server create request before the model builds the server instance.
	 * Implementations may inspect or mutate the attributes map.
	 */
	default CreateServerValidation validateCreate(IServerType type, String id, Map<String, Object> attributes) {
		return new CreateServerValidation(Status.OK_STATUS, Collections.emptyList());
	}

	/**
	 * Invoked before the model instantiates and saves a new server.
	 */
	default void beforeCreate(IServerType type, String id, Map<String, Object> attributes) throws CoreException {
	}

	/**
	 * Invoked after a server has been created and added to the model.
	 */
	default void afterCreate(IServer server) throws CoreException {
	}

	/**
	 * Invoked after a server is loaded from persistence.
	 */
	default void afterLoad(IServer server) throws CoreException {
	}

	/**
	 * Invoked before a server update is applied. Implementations may inspect or mutate the attributes map.
	 */
	default void beforeUpdate(IServer server, Map<String, Object> attributes) throws CoreException {
	}

	/**
	 * Invoked after a server update is applied and saved.
	 */
	default void afterUpdate(IServer server) throws CoreException {
	}

	/**
	 * Invoked before a server is removed from the model and deleted from storage.
	 */
	default void beforeRemove(IServer server) throws CoreException {
	}

	/**
	 * Invoked after a server has been removed and deleted.
	 */
	default void afterRemove(String serverId) throws CoreException {
	}
}
