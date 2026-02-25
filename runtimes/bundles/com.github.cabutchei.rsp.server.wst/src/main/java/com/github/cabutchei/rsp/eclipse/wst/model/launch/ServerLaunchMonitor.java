package com.github.cabutchei.rsp.eclipse.wst.model.launch;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.wst.server.core.ServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.eclipse.wst.model.WSTServerModel;
import com.github.cabutchei.rsp.eclipse.wst.model.delegate.AbstractWstServerDelegate;
import com.github.cabutchei.rsp.eclipse.wst.proxy.WstLaunchProxy;

/**
 * Monitors Eclipse Debug launches globally and attaches stream listeners for WTP-backed servers.
 * This covers starts that bypass delegate start flows (for example publish-triggered starts).
 */
public final class ServerLaunchMonitor {
	private static final Logger LOG = LoggerFactory.getLogger(ServerLaunchMonitor.class);
	private static final ServerLaunchMonitor INSTANCE = new ServerLaunchMonitor();

	private final AtomicBoolean started = new AtomicBoolean(false);
	private volatile WSTServerModel serverModel;
	private ILaunchesListener2 listener;

	private ServerLaunchMonitor() {
	}

	public static ServerLaunchMonitor getInstance() {
		return INSTANCE;
	}

	public void start(WSTServerModel model) {
		this.serverModel = Objects.requireNonNull(model, "model");
		if (!started.compareAndSet(false, true)) {
			// Already running: just update the active model reference.
			return;
		}
		DebugPlugin plugin = DebugPlugin.getDefault();
		if (plugin == null || plugin.getLaunchManager() == null) {
			started.set(false);
			LOG.warn("ServerLaunchMonitor not started: DebugPlugin unavailable.");
			return;
		}
		listener = new ILaunchesListener2() {
			@Override
			public void launchesAdded(org.eclipse.debug.core.ILaunch[] launches) {
				handleLaunches(launches);
			}

			@Override
			public void launchesRemoved(org.eclipse.debug.core.ILaunch[] launches) {
				// no-op
			}

			@Override
			public void launchesChanged(org.eclipse.debug.core.ILaunch[] launches) {
				handleLaunches(launches);
			}

			@Override
			public void launchesTerminated(org.eclipse.debug.core.ILaunch[] launches) {
				// no-op
			}
		};
		plugin.getLaunchManager().addLaunchListener(listener);
		handleLaunches(plugin.getLaunchManager().getLaunches());
		LOG.info("ServerLaunchMonitor started.");
	}

	public void stop() {
		if (!started.compareAndSet(true, false)) {
			serverModel = null;
			return;
		}
		DebugPlugin plugin = DebugPlugin.getDefault();
		if (plugin != null && plugin.getLaunchManager() != null && listener != null) {
			plugin.getLaunchManager().removeLaunchListener(listener);
		}
		listener = null;
		serverModel = null;
		LOG.info("ServerLaunchMonitor stopped.");
	}

	private void handleLaunches(org.eclipse.debug.core.ILaunch[] launches) {
		if (launches == null || launches.length == 0) {
			return;
		}
		for (org.eclipse.debug.core.ILaunch launch : launches) {
			attachIfWtpServerLaunch(launch);
		}
	}

	private void attachIfWtpServerLaunch(org.eclipse.debug.core.ILaunch launch) {
		if (launch == null) {
			return;
		}
		org.eclipse.debug.core.model.IProcess[] processes = launch.getProcesses();
		if (processes == null || processes.length == 0) {
			return;
		}
		WSTServerModel model = serverModel;
		if (model == null) {
			return;
		}
		org.eclipse.wst.server.core.IServer wstServer;
		try {
			if (launch.getLaunchConfiguration() == null) {
				return;
			}
			wstServer = ServerUtil.getServer(launch.getLaunchConfiguration());
		} catch (org.eclipse.core.runtime.CoreException e) {
			return;
		}
		if (wstServer == null || wstServer.getId() == null) {
			return;
		}
		IServer rspServer = model.getServer(wstServer.getId());
		if (rspServer == null) {
			return;
		}
		IServerDelegate delegate = rspServer.getDelegate();
		if (!(delegate instanceof AbstractWstServerDelegate)) {
			return;
		}
		((AbstractWstServerDelegate) delegate).attachLaunchStreamListenersFromMonitor(new WstLaunchProxy(launch));
	}
}
