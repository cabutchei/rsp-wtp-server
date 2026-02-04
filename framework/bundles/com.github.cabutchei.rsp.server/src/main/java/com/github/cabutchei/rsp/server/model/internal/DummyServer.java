/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.model.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.osgi.util.NLS;
import com.github.cabutchei.rsp.launching.memento.IMemento;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;

import com.google.gson.JsonSyntaxException;

public class DummyServer extends Server {

	public static DummyServer createDummyServer(String json, IServerModel smodel) throws CoreException {
		DummyServer ds = new DummyServer();
		ds.loadFromJson(json);
		String serverType = ds.getAttribute(Server.TYPE_ID, (String)null);
		IServerType type = smodel.getIServerType(serverType);
		if( type == null ) {
			throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0, 
					NLS.bind("Server type not found: {0}", serverType), null));
		}
		ds.setServerType(type);
		ds.setDelegate(type.createServerDelegate(ds));
		ds.loadFromJson(json);
		return ds;
	}
	
	public DummyServer() {
		super(null, null, null);
	}
	
	public void loadFromJson(String json) throws CoreException {
		try(InputStream in = new ByteArrayInputStream(json.getBytes())) {
			IMemento memento = null;
			try {
				memento = loadMemento(in); 
			} catch(JsonSyntaxException jse) {
				throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0, 
						NLS.bind("Parse error while reading server string: {0}", 
								jse.getMessage()), null));
			}
			load(memento);
		} catch (IOException | RuntimeException e) {
			throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, 0, 
					NLS.bind("Error while reading server string: {0}", e.getMessage()), e));
		}
	}
	@Override
	public void save(IProgressMonitor monitor) throws CoreException {
		// Do nothing
	}

	public Map<String, Object> getMap() {
		return map;
	}
}
