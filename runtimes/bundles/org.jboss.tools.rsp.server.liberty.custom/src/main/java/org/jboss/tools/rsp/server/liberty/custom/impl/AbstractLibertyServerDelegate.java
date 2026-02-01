/**
 * Copyright (c) 2026 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors: Red Hat, Inc.
 */
package org.jboss.tools.rsp.server.liberty.custom.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.rsp.api.DefaultServerAttributes;
import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.ListServerActionResponse;
import org.jboss.tools.rsp.api.dao.ServerActionRequest;
import org.jboss.tools.rsp.api.dao.ServerActionWorkflow;
import org.jboss.tools.rsp.api.dao.WorkflowResponse;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.server.discovery.serverbeans.ServerBeanLoader;
import org.jboss.tools.rsp.server.model.AbstractServerDelegate;
import org.jboss.tools.rsp.server.spi.servertype.CreateServerValidation;
import org.jboss.tools.rsp.server.spi.servertype.IServer;
import org.jboss.tools.rsp.server.spi.servertype.IServerWorkingCopy;
import org.jboss.tools.rsp.server.spi.util.StatusConverter;
import org.jboss.tools.rsp.server.tomcat.servertype.impl.ILibertyServerAttributes;
import org.jboss.tools.rsp.eclipse.debug.core.ILaunch;
import org.jboss.tools.rsp.server.liberty.custom.servertype.LibertyServerType;

public abstract class AbstractLibertyServerDelegate extends AbstractServerDelegate {
	private static final String DEFAULT_LIBERTY_ID = "defaultServer";
	private static final String DEFAULT_SERVER_HTTP_PORT = LibertyServerType.ATTR_HTTP_PORT;
	private static final String DEFAULT_CLASSPATH_ADDITIONS = LibertyServerType.ATTR_CLASSPATH_ADDITIONS;
	private static final String START_PROGRAM_ARGS = "${server.liberty.id}";
	private static final String STOP_PROGRAM_ARGS = "${server.liberty.id} --stop";
	private static final String VM_ARGS = "-javaagent:${server.home.dir}/bin/tools/ws-javaagent.jar -Djava.awt.headless=true -Djdk.attach.allowAttachSelf=true";

	private ILaunch startLaunch;

	protected AbstractLibertyServerDelegate(IServer server) {
		super(server);
	}

	protected ILaunch getStartLaunch() {
		return startLaunch;
	}

	protected void setStartLaunch(ILaunch launch) {
		this.startLaunch = launch;
	}

	@Override
	public CreateServerValidation validate() {
		String validationType = getServer().getAttribute("server.home.validation", "discovery");
		if ("isFolder".equals(validationType)) {
			return validateServerHomeFolderExists(getServer());
		}
		return validateServerHomeDiscovery(getServer());
	}

	private CreateServerValidation validateServerHomeFolderExists(IServer server) {
		IStatus failed = new Status(IStatus.ERROR, Activator.BUNDLE_ID,
				"Server type not found at given server home");
		String path = getServerHome(server);
		if (path == null) {
			return new CreateServerValidation(failed, List.of(getServerHomeKey()));
		}
		File file = new File(path);
		if (!file.exists() || !file.isDirectory()) {
			return new CreateServerValidation(failed, List.of(getServerHomeKey()));
		}
		return new CreateServerValidation(Status.OK_STATUS, new ArrayList<>());
	}

	private CreateServerValidation validateServerHomeDiscovery(IServer server) {
		IStatus failed = new Status(IStatus.ERROR, Activator.BUNDLE_ID,
				"Server type not found at given server home");
		String path = getServerHome(server);
		if (path == null) {
			return new CreateServerValidation(failed, List.of(getServerHomeKey()));
		}
		ServerBeanLoader loader = new ServerBeanLoader(new File(path), getServer().getServerManagementModel());
		String foundType = loader.getServerAdapterId();
		if (!getServer().getServerType().getId().equals(foundType)) {
			return new CreateServerValidation(failed, List.of(getServerHomeKey()));
		}
		return new CreateServerValidation(Status.OK_STATUS, new ArrayList<>());
	}

	private String getServerHomeKey() {
		String path = getServer().getAttribute(DefaultServerAttributes.SERVER_HOME_DIR, (String) null);
		if (path != null) {
			return DefaultServerAttributes.SERVER_HOME_DIR;
		}
		path = getServer().getAttribute(DefaultServerAttributes.SERVER_HOME_FILE, (String) null);
		if (path != null) {
			return DefaultServerAttributes.SERVER_HOME_FILE;
		}
		return DefaultServerAttributes.SERVER_HOME_DIR;
	}

	protected String getServerHome(IServer server) {
		String home = server.getAttribute(DefaultServerAttributes.SERVER_HOME_DIR, (String) null);
		if (home != null) {
			return home;
		}
		String homeFile = server.getAttribute(DefaultServerAttributes.SERVER_HOME_FILE, (String) null);
		if (homeFile != null) {
			File file = new File(homeFile);
			return file.getParent();
		}
		return null;
	}

	protected String getLibertyServerId(IServerWorkingCopy server) {
		return server.getAttribute(LibertyServerType.ATTR_LIBERTY_ID, DEFAULT_LIBERTY_ID);
	}

	protected String getLibertyServerId() {
		return getServer().getAttribute(LibertyServerType.ATTR_LIBERTY_ID, DEFAULT_LIBERTY_ID);
	}

	protected String getShowInBrowserBaseUrl() {
		String host = getServer().getAttribute(ILibertyServerAttributes.LIBERTY_SERVER_HOST,
				ILibertyServerAttributes.LIBERTY_SERVER_HOST_DEFAULT);
		int port = getServer().getAttribute(ILibertyServerAttributes.LIBERTY_SERVER_PORT,
				ILibertyServerAttributes.LIBERTY_SERVER_PORT_DEFAULT);
		return "http://" + host + ":" + port;
	}

	protected String[] getConfigurationFilePaths() {
		String home = getServerHome(getServer());
		if (home == null) {
			return new String[0];
		}
		String relative = "usr/servers/" + getLibertyServerId() + "/server.xml";
		File file = new File(home, relative);
		if (file.isFile()) {
			return new String[] { file.getAbsolutePath() };
		}
		return new String[0];
	}

	@Override
	public ListServerActionResponse listServerActions() {
		ListServerActionResponse ret = new ListServerActionResponse();
		ret.setStatus(StatusConverter.convert(Status.OK_STATUS));
		List<ServerActionWorkflow> workflows = new ArrayList<>();
		if (getServerRunState() == ServerManagementAPIConstants.STATE_STARTED) {
			workflows.add(new LibertyShowInBrowserActionHandler(this).getInitialWorkflow());
		}
		ServerActionWorkflow edit = new LibertyEditServerConfigurationActionHandler(this).getInitialWorkflow();
		if (edit != null) {
			workflows.add(edit);
		}
		ret.setWorkflows(workflows);
		return ret;
	}

	@Override
	public WorkflowResponse executeServerAction(ServerActionRequest req) {
		if (req == null) {
			return cancelWorkflowResponse();
		}
		if (LibertyShowInBrowserActionHandler.ACTION_ID.equals(req.getActionId())) {
			return new LibertyShowInBrowserActionHandler(this).handle(req);
		}
		if (LibertyEditServerConfigurationActionHandler.ACTION_ID.equals(req.getActionId())) {
			return new LibertyEditServerConfigurationActionHandler(this).handle(req);
		}
		return cancelWorkflowResponse();
	}

	protected String resolveTemplate(IServerWorkingCopy server, String template) {
		if (template == null) {
			return null;
		}
		String result = template;
		String home = server.getAttribute(DefaultServerAttributes.SERVER_HOME_DIR, (String) null);
		if (home == null) {
			home = server.getAttribute(DefaultServerAttributes.SERVER_HOME_FILE, (String) null);
		}
		String libertyId = getLibertyServerId(server);
		String classpathAdditions = server.getAttribute(DEFAULT_CLASSPATH_ADDITIONS, "");
		int port = server.getAttribute(DEFAULT_SERVER_HTTP_PORT, ILibertyServerAttributes.LIBERTY_SERVER_PORT_DEFAULT);
		result = result.replace("${server.home.dir}", home == null ? "" : home);
		result = result.replace("${server.liberty.id}", libertyId == null ? "" : libertyId);
		result = result.replace("${server.classpath.additions}", classpathAdditions == null ? "" : classpathAdditions);
		result = result.replace("${server.http.port}", Integer.toString(port));
		return result;
	}

	protected void setJavaLaunchDependentDefaults(IServerWorkingCopy server) {
		server.setAttribute(org.jboss.tools.rsp.server.generic.servertype.GenericServerType.LAUNCH_OVERRIDE_BOOLEAN, false);
		server.setAttribute(org.jboss.tools.rsp.server.generic.servertype.GenericServerType.LAUNCH_OVERRIDE_PROGRAM_ARGS,
				emptyStringDefault(resolveTemplate(server, START_PROGRAM_ARGS)));
		server.setAttribute(org.jboss.tools.rsp.server.generic.servertype.GenericServerType.JAVA_LAUNCH_OVERRIDE_VM_ARGS,
				emptyStringDefault(resolveTemplate(server, VM_ARGS)));
		server.setAttribute(org.jboss.tools.rsp.server.generic.servertype.GenericServerType.LAUNCH_OVERRIDE_SHUTDOWN_PROGRAM_ARGS,
				emptyStringDefault(resolveTemplate(server, STOP_PROGRAM_ARGS)));
		server.setAttribute(org.jboss.tools.rsp.server.generic.servertype.GenericServerType.JAVA_LAUNCH_OVERRIDE_SHUTDOWN_VM_ARGS,
				emptyStringDefault(resolveTemplate(server, VM_ARGS)));
	}

	@Override
	protected void setServerState(int state) {
		// no-op: WST handles server state notifications
	}

	@Override
	protected void setServerState(int state, boolean fire) {
		// no-op: WST handles server state notifications
	}

	private String emptyStringDefault(String s) {
		return s == null || s.isEmpty() ? "" : s;
	}

	protected abstract String[] getDeploymentUrls(String strategy, String baseUrl, String deployableOutputName,
			DeployableState ds);

	protected String getDeploymentStrategy() {
		return "appendDeploymentNameRemoveSuffix";
	}
}
