package com.github.cabutchei.rsp.server.eap.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

import com.github.cabutchei.rsp.eclipse.debug.core.ILaunch;
import com.github.cabutchei.rsp.eclipse.wst.proxy.WstLaunchProxy;

final class LaunchStreamAttacher {
	private final Object lock = new Object();
	private final String serverId;
	private final Consumer<ILaunch> onLaunchReady;
	private ILaunchesListener2 launchListener;
	private volatile CompletableFuture<ILaunch> launchWithProcessFuture = new CompletableFuture<>();

	LaunchStreamAttacher(String serverId, Consumer<ILaunch> onLaunchReady) {
		this.serverId = Objects.requireNonNull(serverId, "serverId");
		this.onLaunchReady = Objects.requireNonNull(onLaunchReady, "onLaunchReady");
	}

	void attach() {
		registerLaunchListener();
		org.eclipse.debug.core.ILaunch wstLaunch = getWstLaunch();
		tryAttachLaunch(wstLaunch);
	}

	void reset() {
		synchronized (lock) {
			removeLaunchListenerLocked();
			launchWithProcessFuture = new CompletableFuture<>();
		}
	}

	void dispose() {
		reset();
	}

	ILaunch awaitLaunchWithProcess(long timeoutMillis) {
		CompletableFuture<ILaunch> future = launchWithProcessFuture;
		try {
			return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private org.eclipse.debug.core.ILaunch getWstLaunch() {
		org.eclipse.wst.server.core.IServer wstServer = ServerCore.findServer(serverId);
		return wstServer == null ? null : wstServer.getLaunch();
	}

	private boolean tryAttachLaunch(org.eclipse.debug.core.ILaunch wstLaunch) {
		if (wstLaunch == null) {
			return false;
		}
		org.eclipse.debug.core.model.IProcess[] processes = wstLaunch.getProcesses();
		if (processes == null || processes.length == 0) {
			return false;
		}
		ILaunch launch = new WstLaunchProxy(wstLaunch);
		onLaunchReady.accept(launch);
		completeLaunchFuture(launch);
		return true;
	}

	private void registerLaunchListener() {
		synchronized (lock) {
			if (launchListener != null) {
				return;
			}
			launchListener = new ILaunchesListener2() {
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
			DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener);
		}
	}

	private void handleLaunches(org.eclipse.debug.core.ILaunch[] launches) {
		if (launches == null) {
			return;
		}
		for (org.eclipse.debug.core.ILaunch launch : launches) {
			if (!isLaunchForServer(launch)) {
				continue;
			}
			tryAttachLaunch(launch);
		}
	}

	private boolean isLaunchForServer(org.eclipse.debug.core.ILaunch launch) {
		if (launch == null || launch.getLaunchConfiguration() == null) {
			return false;
		}
		try {
			org.eclipse.wst.server.core.IServer wstServer = ServerUtil.getServer(launch.getLaunchConfiguration());
			return wstServer != null && serverId.equals(wstServer.getId());
		} catch (org.eclipse.core.runtime.CoreException e) {
			return false;
		}
	}

	private void completeLaunchFuture(ILaunch launch) {
		if (launch == null) {
			return;
		}
		CompletableFuture<ILaunch> future = launchWithProcessFuture;
		if (future != null && !future.isDone()) {
			future.complete(launch);
		}
	}

	private void removeLaunchListenerLocked() {
		if (launchListener != null) {
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
			launchListener = null;
		}
	}
}
