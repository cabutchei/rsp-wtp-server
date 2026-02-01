/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.liberty.custom.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.ServerActionRequest;
import org.jboss.tools.rsp.api.dao.ServerActionWorkflow;
import org.jboss.tools.rsp.api.dao.WorkflowPromptDetails;
import org.jboss.tools.rsp.api.dao.WorkflowResponse;
import org.jboss.tools.rsp.api.dao.WorkflowResponseItem;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.model.AbstractServerDelegate;
import org.jboss.tools.rsp.server.spi.util.StatusConverter;

public class LibertyEditServerConfigurationActionHandler {
	public static final String ACTION_ID = "EditServerConfigurationActionHandler.actionId";
	public static final String ACTION_LABEL = "Edit Configuration File...";
	public static final String ACTION_EDIT_FILE_PROMPT_ID = "EditServerConfigurationActionHandler.selection.id";
	public static final String ACTION_EDIT_FILE_PROMPT_LABEL = "EditServerConfigurationActionHandler.selection.label";

	private final AbstractLibertyServerDelegate delegate;

	public LibertyEditServerConfigurationActionHandler(AbstractLibertyServerDelegate delegate) {
		this.delegate = delegate;
	}

	public ServerActionWorkflow getInitialWorkflow() {
		String[] files = delegate.getConfigurationFilePaths();
		if (files == null || files.length == 0) {
			return cancelWorkflow();
		}
		if (files.length == 1) {
			return executePath(files[0]);
		}
		return fileChoiceWorkflow(files);
	}

	private ServerActionWorkflow fileChoiceWorkflow(String[] choices) {
		WorkflowResponse workflow = new WorkflowResponse();
		ServerActionWorkflow action = new ServerActionWorkflow(ACTION_ID, ACTION_LABEL, workflow);

		List<WorkflowResponseItem> items = new ArrayList<>();
		WorkflowPromptDetails prompt = new WorkflowPromptDetails();
		prompt.setResponseSecret(false);
		prompt.setResponseType(ServerManagementAPIConstants.ATTR_TYPE_STRING);
		prompt.setValidResponses(Arrays.asList(choices));

		WorkflowResponseItem item = new WorkflowResponseItem();
		item.setItemType(ServerManagementAPIConstants.WORKFLOW_TYPE_PROMPT_SMALL);
		item.setPrompt(prompt);
		item.setId(ACTION_EDIT_FILE_PROMPT_ID);
		item.setLabel(ACTION_EDIT_FILE_PROMPT_LABEL);

		items.add(item);
		workflow.setItems(items);
		workflow.setStatus(StatusConverter.convert(
				new Status(IStatus.INFO, Activator.BUNDLE_ID, ACTION_LABEL)));
		return action;
	}

	private ServerActionWorkflow cancelWorkflow() {
		WorkflowResponse workflow = new WorkflowResponse();
		ServerActionWorkflow action = new ServerActionWorkflow(ACTION_ID, ACTION_LABEL, workflow);
		workflow.setItems(new ArrayList<>());
		workflow.setStatus(StatusConverter.convert(
				new Status(IStatus.CANCEL, Activator.BUNDLE_ID, ACTION_LABEL)));
		return action;
	}

	private ServerActionWorkflow executePath(String absolutePath) {
		if (absolutePath == null) {
			return cancelWorkflow();
		}
		WorkflowResponseItem item = new WorkflowResponseItem();
		item.setItemType(ServerManagementAPIConstants.WORKFLOW_TYPE_OPEN_EDITOR);
		Map<String, String> propMap = new HashMap<>();
		propMap.put(ServerManagementAPIConstants.WORKFLOW_EDITOR_PROPERTY_PATH, absolutePath);
		item.setProperties(propMap);
		item.setId(ACTION_ID);
		item.setLabel(ACTION_LABEL);

		WorkflowResponse workflow = new WorkflowResponse();
		ServerActionWorkflow action = new ServerActionWorkflow(ACTION_ID, ACTION_LABEL, workflow);
		workflow.setItems(List.of(item));
		workflow.setStatus(StatusConverter.convert(
				new Status(IStatus.OK, Activator.BUNDLE_ID, ACTION_LABEL)));
		return action;
	}

	public WorkflowResponse handle(ServerActionRequest req) {
		if (req == null || req.getData() == null || req.getData().isEmpty()) {
			return AbstractServerDelegate.okWorkflowResponse();
		}
		String path = (String) req.getData().get(ACTION_EDIT_FILE_PROMPT_ID);
		if (path != null) {
			return executePath(path).getActionWorkflow();
		}
		return cancelWorkflow().getActionWorkflow();
	}
}
