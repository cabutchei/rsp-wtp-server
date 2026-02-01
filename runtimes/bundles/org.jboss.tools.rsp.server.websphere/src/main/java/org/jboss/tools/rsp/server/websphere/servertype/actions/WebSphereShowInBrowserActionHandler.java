/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.websphere.servertype.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.ServerActionRequest;
import org.jboss.tools.rsp.api.dao.ServerActionWorkflow;
import org.jboss.tools.rsp.api.dao.WorkflowPromptDetails;
import org.jboss.tools.rsp.api.dao.WorkflowResponse;
import org.jboss.tools.rsp.api.dao.WorkflowResponseItem;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.model.AbstractServerDelegate;
import org.jboss.tools.rsp.server.spi.util.StatusConverter;
import org.jboss.tools.rsp.server.websphere.impl.AbstractWebSphereServerDelegate;
import org.jboss.tools.rsp.server.websphere.impl.Activator;

public class WebSphereShowInBrowserActionHandler {
	public static final String ACTION_ID = "WebSphereShowInBrowserActionHandler.actionId";
	public static final String ACTION_LABEL = "Show in browser...";
	public static final String ACTION_SELECTED_PROMPT_ID = "WebSphereShowInBrowserActionHandler.selection.id";
	public static final String ACTION_SELECTED_PROMPT_LABEL =
			"Which deployment do you want to show in the web browser?";

	private final AbstractWebSphereServerDelegate delegate;

	public WebSphereShowInBrowserActionHandler(AbstractWebSphereServerDelegate delegate) {
		this.delegate = delegate;
	}

	public ServerActionWorkflow getInitialWorkflow() {
		WorkflowResponse workflow = new WorkflowResponse();
		workflow.setStatus(StatusConverter.convert(
				new Status(IStatus.INFO, Activator.BUNDLE_ID, ACTION_LABEL)));
		ServerActionWorkflow action = new ServerActionWorkflow(ACTION_ID, ACTION_LABEL, workflow);

		List<WorkflowResponseItem> items = new ArrayList<>();
		WorkflowPromptDetails prompt = new WorkflowPromptDetails();
		prompt.setResponseSecret(false);
		prompt.setResponseType(ServerManagementAPIConstants.ATTR_TYPE_STRING);
		prompt.setValidResponses(getDeploymentChoices());

		WorkflowResponseItem item = new WorkflowResponseItem();
		item.setItemType(ServerManagementAPIConstants.WORKFLOW_TYPE_PROMPT_SMALL);
		item.setId(ACTION_SELECTED_PROMPT_ID);
		item.setLabel(ACTION_SELECTED_PROMPT_LABEL);
		item.setPrompt(prompt);
		items.add(item);
		workflow.setItems(items);

		return action;
	}

	private List<String> getDeploymentChoices() {
		List<String> urls = new ArrayList<>();
		String baseUrl = delegate.getShowInBrowserBaseUrl();
		if (baseUrl != null && !baseUrl.isEmpty()) {
			urls.add(baseUrl);
		}
		for (DeployableState ds : getDeployableStates()) {
			String[] depUrls = getDeploymentUrls(ds, baseUrl);
			if (depUrls != null) {
				urls.addAll(Arrays.asList(depUrls));
			}
		}
		return urls;
	}

	private List<DeployableState> getDeployableStates() {
		return delegate.getServerPublishModel().getDeployableStatesWithOptions();
	}

	private String[] getDeploymentUrls(DeployableState ds, String baseUrl) {
		String strategy = delegate.getDeploymentStrategy();
		String outputName = getOutputName(ds.getReference());
		String[] fromDelegate = delegate.getDeploymentUrls(strategy, baseUrl, outputName, ds);
		if (fromDelegate != null) {
			return fromDelegate;
		}
		String depName = outputName;
		if ("appendDeploymentNameRemoveSuffix".equals(strategy)) {
			int dot = depName == null ? -1 : depName.lastIndexOf('.');
			if (dot > 0) {
				depName = depName.substring(0, dot);
			}
		}
		if (baseUrl == null || depName == null) {
			return new String[0];
		}
		return new String[] { baseUrl + "/" + depName };
	}

	private String getOutputName(DeployableReference ref) {
		Map<String, Object> options = ref.getOptions();
		String def = null;
		if (ref.getPath() != null) {
			def = new File(ref.getPath()).getName();
		}
		String key = ServerManagementAPIConstants.DEPLOYMENT_OPTION_OUTPUT_NAME;
		if (options != null && options.get(key) != null) {
			return (String) options.get(key);
		}
		return def;
	}

	public WorkflowResponse handle(ServerActionRequest req) {
		if (req == null || req.getData() == null) {
			return AbstractServerDelegate.cancelWorkflowResponse();
		}
		String choice = (String) req.getData().get(ACTION_SELECTED_PROMPT_ID);
		if (choice == null) {
			return AbstractServerDelegate.cancelWorkflowResponse();
		}
		String url = findUrlFromChoice(choice);
		if (url != null) {
			WorkflowResponseItem item = new WorkflowResponseItem();
			item.setItemType(ServerManagementAPIConstants.WORKFLOW_TYPE_OPEN_BROWSER);
			item.setLabel("Open the following url: " + url);
			item.setContent(url);
			WorkflowResponse resp = new WorkflowResponse();
			resp.setItems(Arrays.asList(item));
			resp.setStatus(StatusConverter.convert(Status.OK_STATUS));
			return resp;
		}
		return AbstractServerDelegate.cancelWorkflowResponse();
	}

	private String findUrlFromChoice(String choice) {
		if (choice == null) {
			return null;
		}
		String lower = choice.toLowerCase();
		if (lower.startsWith("http://") || lower.startsWith("https://")) {
			return choice;
		}
		return null;
	}
}
