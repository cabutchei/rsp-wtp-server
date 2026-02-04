/*************************************************************************************
 * Copyright (c) 2013-2018 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package com.github.cabutchei.rsp.stacks.core;

import com.github.cabutchei.rsp.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StacksCoreActivator implements BundleActivator {

	public static final String PLUGIN_ID = "com.github.cabutchei.rsp.stacks.core";
	private static final Logger LOG = LoggerFactory.getLogger(StacksCoreActivator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		LOG.debug(NLS.bind("{0} bundle started.", PLUGIN_ID));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.debug(NLS.bind("{0} bundle stopped.", PLUGIN_ID));
	}
	
}