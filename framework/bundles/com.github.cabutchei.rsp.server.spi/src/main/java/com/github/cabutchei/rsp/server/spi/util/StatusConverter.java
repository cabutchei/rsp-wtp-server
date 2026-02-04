/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.MultiStatus;

public class StatusConverter {
	public static com.github.cabutchei.rsp.eclipse.core.runtime.IStatus convert(
			com.github.cabutchei.rsp.api.dao.Status status) {
		int sev = status.getSeverity();
		String plugin = status.getPlugin();
		String msg = status.getMessage();
		return new com.github.cabutchei.rsp.eclipse.core.runtime.Status(sev, plugin, msg);
	}
	
	public static com.github.cabutchei.rsp.api.dao.Status convert(
			com.github.cabutchei.rsp.eclipse.core.runtime.IStatus status) {
		if (status == null) {
			return null;
		}
		StringBuffer fullMessage = new StringBuffer();

		int sev = status.getSeverity();
		String plugin = status.getPlugin();
		String msg = status.getMessage();
		Throwable t = status.getException();
		String trace = getTrace(t);
		if( trace != null )
			fullMessage.append(trace);
		
		if( status instanceof MultiStatus ) {
			IStatus[] kids = ((MultiStatus)status).getChildren();
			if( kids != null ) {
				for( int i = 0; i < kids.length; i++ ) {
					String msg2 = kids[i].getMessage();
					Throwable t2 = kids[i].getException();
					String trace2 = getTrace(t2);
					fullMessage.append("\n\n");
					if( msg2 != null ) {
						fullMessage.append(msg2);
						fullMessage.append("\n");
					}
					if( trace2 != null )
						fullMessage.append(trace2);
				}
			}
		}
		
		com.github.cabutchei.rsp.api.dao.Status ret = 
				new com.github.cabutchei.rsp.api.dao.Status(sev, plugin, msg, fullMessage.toString());
		return ret;
	}
	
	private static String getTrace(Throwable t) {
		String trace = null;
		if( t != null ) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			trace = sw.toString(); // stack trace as a string
		}
		return trace;

	}
}
