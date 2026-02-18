/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package com.github.cabutchei.rsp.eclipse.wst.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.BooleanSupplier;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.ServerState;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;

/**
 * WST copy of the auto-publish thread with public status accessors.
 */
public class WSTAutoPublishThread extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(WSTAutoPublishThread.class);

	private int maxInactive = 1;
	private final IServer server;
	private final BooleanSupplier hasPendingChanges;
	private final BooleanSupplier isAutoPublishEnabled;
	private boolean publishBegan;
	private boolean done;
	private boolean canceled;
	private long lastUpdated;

	public WSTAutoPublishThread(IServer server, int ms, BooleanSupplier hasPendingChanges, BooleanSupplier isAutoPublishEnabled) {
		this.server = server;
		this.maxInactive = ms;
		this.hasPendingChanges = hasPendingChanges;
		this.isAutoPublishEnabled = isAutoPublishEnabled;
		this.publishBegan = false;
		this.done = false;
		this.canceled = false;
		this.lastUpdated = System.currentTimeMillis();
		setDaemon(true);
		setPriority(Thread.MIN_PRIORITY + 1);
	}

	@Override
	public void run() {
		boolean shouldPublish = awaitInactivity();
		if (shouldPublish) {
			publishImpl();
			setDone();
		}
	}

	protected void publishImpl() {
		try {
			server.getServerModel().publish(server, ServerManagementAPIConstants.PUBLISH_INCREMENTAL);
		} catch (CoreException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * Await an inactive state for a certain duration.
	 * @return true if should publish, false if should abort thread
	 */
	protected boolean awaitInactivity() {
		// Don't even wait, if state is garbage just abort now
		if (shouldAbort()) {
			setDone();
			return false;
		}

		while (!getPublishBegan()) {
			long preSleepLastUpdated = getLastUpdated();
			sleepExpectedDuration();
			if (shouldAbort()) {
				setDone();
				return false;
			}
			synchronized (this) {
				if (getLastUpdated() != preSleepLastUpdated) {
					// While we slept, someone updated another file,
					// which means we need to wait longer
					continue;
				}
				setPublishBegan();
			}
		}
		return true;
	}

	protected boolean shouldAbort() {
		if (isCanceled()) {
			return true;
		}
		if (isAutoPublishEnabled != null && !isAutoPublishEnabled.getAsBoolean()) {
			return true;
		}
		ServerState state = getServerState();
		int runState = state.getState();
		int publishState = state.getPublishState();
		if (runState != ServerManagementAPIConstants.STATE_STARTED
				|| (publishState == ServerManagementAPIConstants.PUBLISH_STATE_NONE
					&& (hasPendingChanges == null || !hasPendingChanges.getAsBoolean()))) {
			return true;
		}
		return false;
	}

	public synchronized void cancel() {
		this.canceled = true;
		interrupt();
	}

	private synchronized boolean isCanceled() {
		return this.canceled;
	}

	protected ServerState getServerState() {
		return server.getDelegate().getServerState();
	}

	/**
	 * Sleep the duration expected to reach our cutoff for filesystem silence.
	 */
	protected void sleepExpectedDuration() {
		try {
			long curTime = System.currentTimeMillis();
			long nextSleep = getAwakenTime() - curTime;
			if (nextSleep > 0) {
				sleep(nextSleep);
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	public synchronized void updateInactivityCounter() {
		this.lastUpdated = System.currentTimeMillis();
	}

	protected synchronized long getLastUpdated() {
		return this.lastUpdated;
	}

	protected long getAwakenTime() {
		return getLastUpdated() + maxInactive;
	}

	protected synchronized void setPublishBegan() {
		this.publishBegan = true;
	}

	public synchronized boolean getPublishBegan() {
		return this.publishBegan;
	}

	protected synchronized void setDone() {
		this.done = true;
	}

	public synchronized boolean isDone() {
		return this.done;
	}
}
