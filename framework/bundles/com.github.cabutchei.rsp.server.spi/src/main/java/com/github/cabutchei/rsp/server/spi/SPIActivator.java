/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.server.spi;

import com.github.cabutchei.rsp.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPIActivator implements BundleActivator {

	public static final String BUNDLE_ID = "com.github.cabutchei.rsp.server.spi";
	private static final Logger LOG = LoggerFactory.getLogger(SPIActivator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		LOG.debug(NLS.bind("{0} bundle started.", BUNDLE_ID));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.debug(NLS.bind("{0} bundle stopped.", BUNDLE_ID));
	}
}
