package com.github.cabutchei.rsp.eclipse.workspace;


import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;


public class EclipseWorkspaceSrvc {
	private static final long DEFAULT_WORKSPACE_WAIT_MS = 50000;
	private static final BundleContext BUNDLE_CONTEXT = FrameworkUtil.getBundle(IWorkspace.class).getBundleContext();

	public EclipseWorkspaceSrvc() {
		return;
	}

	public static IWorkspace getWorkspace() {
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(BUNDLE_CONTEXT, IWorkspace.class, null);
		tracker.open();
		IWorkspace ws = tracker.getService();
		tracker.close();
		return ws;
	}

	public static IWorkspaceRoot getWorkspaceRoot() {
		return EclipseWorkspaceSrvc.getWorkspace().getRoot();
	}

	public static boolean isAvailable() {
		return getWorkspace() != null;
	}

	public static CompletableFuture<Void> whenAvailable() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(BUNDLE_CONTEXT, IWorkspace.class, null);
		Thread waiter = new Thread(() -> {
			tracker.open();
			try {
				IWorkspace workspace = tracker.waitForService(DEFAULT_WORKSPACE_WAIT_MS);
				if (workspace != null) {
					future.complete(null);
				} else {
					future.completeExceptionally(new IllegalStateException("Workspace service unavailable"));
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				future.completeExceptionally(e);
			} finally {
				tracker.close();
			}
		}, "EclipseWorkspaceSrvc Availability Waiter");
		waiter.setDaemon(true);
		waiter.start();
		return future;
	}

	public static void whenAvailable(Runnable runnable) {
		whenAvailable().thenRun(runnable);
	}

	public static IWorkspace awaitWorkspace(long timeout) {
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(BUNDLE_CONTEXT, IWorkspace.class, null);
		tracker.open();
		try {
			IWorkspace ws = tracker.waitForService(timeout);
			return ws;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		} finally {
			tracker.close();	
		}
	}

	// TODO: static initialize doesn't make any sense
	public static void initialize() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		// Disable auto-building by default since it can cause unexpected builds.
		desc.setAutoBuilding(false);
		workspace.setDescription(desc);
	}
}

