package com.github.cabutchei.rsp.server.websphere.servertype.actions;

import java.util.ArrayList;
import java.util.List;

import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.ServerActionRequest;
import com.github.cabutchei.rsp.api.dao.ServerActionWorkflow;
import com.github.cabutchei.rsp.api.dao.WorkflowResponse;
import com.github.cabutchei.rsp.api.dao.WorkflowResponseItem;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.model.AbstractServerDelegate;
import com.github.cabutchei.rsp.server.spi.util.StatusConverter;
import com.github.cabutchei.rsp.server.websphere.impl.AbstractWebSphereServerDelegate;
import com.github.cabutchei.rsp.server.websphere.impl.Activator;
import com.github.cabutchei.rsp.server.websphere.impl.WebSphereWstServerAccess;

public class WebSphereOpenAdminConsoleActionHandler {
	public static final String ACTION_ID = "WebSphereOpenAdminConsoleActionHandler.actionId";
	public static final String ACTION_LABEL = "Open Admin Console";
	private static final String ADMIN_CONSOLE_PATH = "/ibm/console";

	private final AbstractWebSphereServerDelegate delegate;

	public WebSphereOpenAdminConsoleActionHandler(AbstractWebSphereServerDelegate delegate) {
		this.delegate = delegate;
	}

	public ServerActionWorkflow getInitialWorkflow() {
		WorkflowResponse workflow = new WorkflowResponse();
		ServerActionWorkflow action = new ServerActionWorkflow(ACTION_ID, ACTION_LABEL, workflow);
		List<WorkflowResponseItem> items = new ArrayList<>();
		workflow.setItems(items);

		String adminConsoleUrl = getAdminConsoleUrl();
		if (adminConsoleUrl == null || adminConsoleUrl.isEmpty()) {
			workflow.setStatus(StatusConverter.convert(
					new Status(IStatus.CANCEL, Activator.BUNDLE_ID, ACTION_LABEL)));
			return action;
		}

		WorkflowResponseItem item = new WorkflowResponseItem();
		item.setItemType(ServerManagementAPIConstants.WORKFLOW_TYPE_OPEN_BROWSER);
		item.setId(ACTION_ID);
		item.setLabel("Open the WebSphere admin console");
		item.setContent(adminConsoleUrl);
		items.add(item);
		workflow.setStatus(StatusConverter.convert(
				new Status(IStatus.OK, Activator.BUNDLE_ID, ACTION_LABEL)));
		return action;
	}

	private String getAdminConsoleUrl() {
		try {
			String host = WebSphereWstServerAccess.getServerAdminHostName(delegate.getServer());
			int port = WebSphereWstServerAccess.getAdminConsolePortNum(delegate.getServer());
			if (host == null || host.isEmpty() || port <= 0) {
				return null;
			}
			return "http://" + host + ":" + port + ADMIN_CONSOLE_PATH;
		} catch (CoreException e) {
			return null;
		}
	}

	public WorkflowResponse handle(ServerActionRequest req) {
		if (req == null || req.getData() == null) {
			return AbstractServerDelegate.okWorkflowResponse();
		}
		return AbstractServerDelegate.okWorkflowResponse();
	}
}
