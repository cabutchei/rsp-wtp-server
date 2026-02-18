package com.github.cabutchei.rsp.eclipse.workspace;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;

import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceInitializationRequest;
import com.github.cabutchei.rsp.server.spi.workspace.WorkspaceInitializationSnapshot;

/**
 * Temporary compatibility wrapper. Prefer injecting {@link EclipseWorkspaceService}
 * directly.
 */
public class EclipseWorkspaceSrvc {
	private static final EclipseWorkspaceService SERVICE = new EclipseWorkspaceService();

	private EclipseWorkspaceSrvc() {
	}

	public static IWorkspace getWorkspace() {
		return SERVICE.getWorkspace();
	}

	public static IWorkspaceRoot getWorkspaceRoot() {
		return SERVICE.getWorkspaceRoot();
	}

	public static boolean isAvailable() {
		return SERVICE.isAvailable();
	}

	public static CompletableFuture<IWorkspace> whenAvailable() {
		return SERVICE.whenAvailable();
	}

	public static void whenAvailable(Runnable runnable) {
		if (runnable == null) {
			return;
		}
		SERVICE.whenAvailable(workspace -> runnable.run());
	}

	public static IWorkspace awaitWorkspace(long timeoutMs) {
		return SERVICE.awaitWorkspace(timeoutMs);
	}

	public static IStatus initialize(String clientId, WorkspaceInitializationRequest request) {
		return SERVICE.applyInitialization(clientId, request);
	}

	public static IStatus releaseInitialization(String clientId) {
		return SERVICE.releaseInitialization(clientId);
	}

	public static WorkspaceInitializationSnapshot snapshot() {
		return SERVICE.snapshot();
	}
}
