/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.model.internal;

import java.util.List;

import com.github.cabutchei.rsp.api.ICapabilityKeys;
import com.github.cabutchei.rsp.api.RSPWTPClient;
import com.github.cabutchei.rsp.api.dao.MessageBoxNotification;
import com.github.cabutchei.rsp.server.LauncherSingleton;
import com.github.cabutchei.rsp.server.spi.model.ICapabilityManagement;

public class MessageBoxNotificationManager {
	public static void messageAllClients(MessageBoxNotification message) {
		List<RSPWTPClient> clients = LauncherSingleton.getDefault().getLauncher().getClients();
		for( RSPWTPClient c : clients ) {
			messageClient(c, message);
		}
	}

	public static void messageClient(RSPWTPClient client, MessageBoxNotification message) {
		ICapabilityManagement mgmt = LauncherSingleton.getDefault().getLauncher().getModel().getCapabilityManagement();
		String val = mgmt.getCapabilityProperty(client, ICapabilityKeys.BOOLEAN_MESSAGEBOX);
		if( val != null && Boolean.parseBoolean(val)) {
			client.messageBox(message);
		}
	}
}
