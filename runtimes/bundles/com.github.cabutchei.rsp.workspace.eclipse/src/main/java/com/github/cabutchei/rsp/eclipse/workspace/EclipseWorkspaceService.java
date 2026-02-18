package com.github.cabutchei.rsp.eclipse.workspace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceInitializationService;
import com.github.cabutchei.rsp.server.spi.workspace.IWorkspaceService;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceInitializationRequest;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceInitializationSnapshot;

/**
 * Thin workspace service for availability/wait semantics and initialization policy
 * application.
 */
public class EclipseWorkspaceService implements IWorkspaceService, IWorkspaceInitializationService {
	private static final String BUNDLE_ID = "com.github.cabutchei.rsp.workspace.eclipse";
	private static final long DEFAULT_WORKSPACE_WAIT_MS = 50000;

	private final Object initLock = new Object();
	private final Map<String, WorkspaceInitializationRequest> clientRequests = new LinkedHashMap<>();
	private String ownerClientId;

	@Override
	public IWorkspace getWorkspace() {
		BundleContext context = getBundleContext();
		if (context == null) {
			return null;
		}
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(context, IWorkspace.class, null);
		tracker.open();
		try {
			return tracker.getService();
		} finally {
			tracker.close();
		}
	}

	@Override
	public IWorkspaceRoot getWorkspaceRoot() {
		IWorkspace workspace = getWorkspace();
		return workspace == null ? null : workspace.getRoot();
	}

	@Override
	public boolean isAvailable() {
		return getWorkspace() != null;
	}

	@Override
	public CompletableFuture<IWorkspace> whenAvailable() {
		IWorkspace existing = getWorkspace();
		if (existing != null) {
			return CompletableFuture.completedFuture(existing);
		}
		CompletableFuture<IWorkspace> future = new CompletableFuture<>();
		BundleContext context = getBundleContext();
		if (context == null) {
			future.completeExceptionally(new IllegalStateException("Workspace service unavailable"));
			return future;
		}
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(context, IWorkspace.class, null);
		Thread waiter = new Thread(() -> {
			tracker.open();
			try {
				IWorkspace workspace = tracker.waitForService(DEFAULT_WORKSPACE_WAIT_MS);
				if (workspace != null) {
					future.complete(workspace);
				} else {
					future.completeExceptionally(new IllegalStateException("Workspace service unavailable"));
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				future.completeExceptionally(e);
			} finally {
				tracker.close();
			}
		}, "EclipseWorkspaceService Availability Waiter");
		waiter.setDaemon(true);
		waiter.start();
		return future;
	}

	@Override
	public void whenAvailable(Consumer<IWorkspace> operation) {
		if (operation == null) {
			return;
		}
		whenAvailable().thenAccept(operation);
	}

	@Override
	public IWorkspace awaitWorkspace(long timeoutMs) {
		BundleContext context = getBundleContext();
		if (context == null) {
			return null;
		}
		ServiceTracker<IWorkspace, IWorkspace> tracker = new ServiceTracker<>(context, IWorkspace.class, null);
		tracker.open();
		try {
			return tracker.waitForService(timeoutMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} finally {
			tracker.close();
		}
	}

	@Override
	public IStatus applyInitialization(String clientId, WorkspaceInitializationRequest request) {
		if (clientId == null || clientId.trim().isEmpty()) {
			return errorStatus("Client id cannot be null or empty", null);
		}
		if (request == null) {
			return errorStatus("Initialization request cannot be null", null);
		}
		synchronized (initLock) {
			if (ownerClientId != null && !ownerClientId.equals(clientId)) {
				return errorStatus("Workspace initialization is owned by client " + ownerClientId, null);
			}
			ownerClientId = clientId;
			clientRequests.put(clientId, request);
		}
		return reapplyEffectivePolicy();
	}

	@Override
	public IStatus releaseInitialization(String clientId) {
		if (clientId == null || clientId.trim().isEmpty()) {
			return errorStatus("Client id cannot be null or empty", null);
		}
		synchronized (initLock) {
			clientRequests.remove(clientId);
			if (clientId.equals(ownerClientId)) {
				ownerClientId = null;
				if (!clientRequests.isEmpty()) {
					ownerClientId = clientRequests.keySet().iterator().next();
				}
			}
		}
		return reapplyEffectivePolicy();
	}

	@Override
	public WorkspaceInitializationSnapshot snapshot() {
		synchronized (initLock) {
			WorkspaceInitializationRequest effective = ownerClientId == null ? null : clientRequests.get(ownerClientId);
			return new WorkspaceInitializationSnapshot(ownerClientId, effective, clientRequests);
		}
	}

	@Override
	public IStatus reapplyEffectivePolicy() {
		WorkspaceInitializationRequest effective = snapshot().getEffectiveRequest();
		if (effective == null) {
			return Status.OK_STATUS;
		}
		IWorkspace workspace = getWorkspace();
		if (workspace == null) {
			return Status.OK_STATUS;
		}
		Boolean autoBuilding = effective.getAutoBuilding();
		if (autoBuilding == null) {
			return Status.OK_STATUS;
		}
		try {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(autoBuilding.booleanValue());
			workspace.setDescription(description);
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			return errorStatus("Failed to apply workspace initialization policy", ce);
		}
	}

	private BundleContext getBundleContext() {
		Bundle bundle = FrameworkUtil.getBundle(IWorkspace.class);
		return bundle == null ? null : bundle.getBundleContext();
	}

	private IStatus errorStatus(String message, Throwable t) {
		return new Status(IStatus.ERROR, BUNDLE_ID, message, t);
	}
}
