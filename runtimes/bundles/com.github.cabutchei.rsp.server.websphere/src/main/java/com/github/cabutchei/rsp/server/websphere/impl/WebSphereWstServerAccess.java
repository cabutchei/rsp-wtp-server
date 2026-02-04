package com.github.cabutchei.rsp.server.websphere.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;

import com.github.cabutchei.rsp.eclipse.wst.IWstIntegrationService;
import com.github.cabutchei.rsp.eclipse.wst.WSTServerContext;

import com.ibm.ws.ast.st.v85.core.internal.WASServer;
import com.ibm.ws.ast.st.v85.core.internal.jmx.WASConfigModelHelper;
import com.ibm.ws.ast.st.v85.core.internal.util.ServerXmlFileHandler;
import com.ibm.ws.ast.st.core.internal.util.IMemento;
import com.ibm.ws.ast.st.core.internal.util.XMLMemento;



public final class WebSphereWstServerAccess {
	private WebSphereWstServerAccess() {
		// utility class
	}

	private static final AtomicLong XMI_COUNTER = new AtomicLong();

	public static ServerXmlFileHandler createServerXmlFileHandler(WSTServerContext context) throws IOException, CoreException  {
		WASServer server;
		server = getWASServer(context);
		return createServerXmlFileHandler(server.getWebSphereInstallPath(), server.getProfileName(), server.getBaseServerName());
	}

	public static ServerXmlFileHandler createServerXmlFileHandler(String curWASInstallRoot, String profileName, String serverName) throws IOException {
		return ServerXmlFileHandler.create(curWASInstallRoot, profileName, serverName);
	}

	public static int getDebugPortNum(WSTServerContext context) throws CoreException {
		WASServer server = getWASServer(context);
		return server.getDebugPortNum();
	}

	public static int getDebugPort(WSTServerContext context) {
		try {
			return createServerXmlFileHandler(context).getDebugPortNum();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String getWebSphereInstallPath(WSTServerContext context) throws CoreException {
		return getWASServer(context).getWebSphereInstallPath();
	}

	public static String getProfileName(WSTServerContext context) throws CoreException {
		return getWASServer(context).getProfileName();
	}

	public static String getBaseServerName(WSTServerContext context) throws CoreException {
		return getWASServer(context).getBaseServerName();
	}

	public static String getServerXmlFilePath(WSTServerContext context) throws IOException, CoreException {
		return createServerXmlFileHandler(context).getServerXMLFilePath();
	}

	public static void setSystemProperties(WSTServerContext context, Map<String, String> systemProperties) throws CoreException {
		try {
			ServerXmlFileHandler handler = createServerXmlFileHandler(context);
			IMemento jvmEntry = handler.getJavaVirtualMachine();
			if (jvmEntry == null) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
						"Unable to resolve JVM entry in server.xml"));
			}

			Map<String, IMemento> existing = new HashMap<>();
			IMemento[] children = jvmEntry.getChildren("systemProperties");
			if (children != null) {
				for (IMemento child : children) {
					String name = child.getString("name");
					if (name != null && !name.isEmpty()) {
						existing.put(name, child);
					}
				}
			}

			Map<String, String> desired = systemProperties == null ? new HashMap<>() : systemProperties;
			for (Map.Entry<String, String> entry : desired.entrySet()) {
				String name = entry.getKey();
				if (name == null || name.trim().isEmpty()) {
					continue;
				}
				String value = entry.getValue() == null ? "" : entry.getValue();
				IMemento target = existing.remove(name);
				if (target == null) {
					target = jvmEntry.createChild("systemProperties");
					target.putString("name", name);
					target.putBoolean("required", false);
					target.putString("xmi:id", generatePropertyXmiId());
				}
				target.putString("value", value);
			}

			if (!existing.isEmpty() && jvmEntry instanceof XMLMemento) {
				XMLMemento xml = (XMLMemento) jvmEntry;
				for (String name : existing.keySet()) {
					xml.removeCustomProperty(name);
				}
			}
			handler.save();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
					"Unable to update WebSphere server.xml", e));
		}
	}

	public static Map<String, String> getSystemProperties(WSTServerContext context) throws CoreException {
		try {
			IMemento jvmEntry = createServerXmlFileHandler(context).getJavaVirtualMachine();
			Map<String, String> systemProperties = new HashMap<>();
			if (jvmEntry != null) {
				IMemento[] children = jvmEntry.getChildren("systemProperties");
				if (children != null) {
					for (IMemento child : children) {
						String name = child.getString("name");
						if (name == null || name.isEmpty()) {
							continue;
						}
						String value = child.getString("value");
						systemProperties.put(name, value == null ? "" : value);
					}
				}
			}
			return systemProperties;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
					"Unable to read WebSphere server.xml", e));
		}
	}

	private static String generatePropertyXmiId() {
		long now = System.currentTimeMillis();
		long seq = XMI_COUNTER.incrementAndGet();
		return "CustomProperty_" + now + "_" + seq;
	}

	public static WASServer getWASServer(WSTServerContext context) throws CoreException {
		if( context == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "Missing WST server context"));
		}
		IWstIntegrationService integration = Activator.getWstIntegrationService();
		if( integration == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "WST integration service unavailable"));
		}
		String serverId = context.getServerHandle().getId();
		org.eclipse.wst.server.core.IServer wstServer = integration.getFacade().getWstServer(serverId);
		if( wstServer == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "WST server not found for id " + serverId));
		}
		WASServer wasServer = (WASServer) wstServer.loadAdapter(WASServer.class, null);
		if( wasServer == null ) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID, "Unable to load WASServer adapter"));
		}
		return wasServer;
	}
}
