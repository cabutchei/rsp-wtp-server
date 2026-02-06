package com.github.cabutchei.rsp.server.websphere.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;

import com.github.cabutchei.rsp.eclipse.wst.IWstServerDelegateAccess;
import com.github.cabutchei.rsp.server.spi.servertype.IServerAttributes;
import com.ibm.ws.ast.st.v85.core.internal.WASServer;
import com.ibm.ws.ast.st.v85.core.internal.util.ServerXmlFileHandler;
import com.ibm.ws.ast.st.core.internal.util.IMemento;
import com.ibm.ws.ast.st.core.internal.util.XMLMemento;



public final class WebSphereWstServerAccess implements IWstServerDelegateAccess<WASServer> {
	private WebSphereWstServerAccess() {
		// utility class
	}

	private static final AtomicLong XMI_COUNTER = new AtomicLong();
	public static final WebSphereWstServerAccess INSTANCE = new WebSphereWstServerAccess();

	@Override
	public Class<WASServer> getDelegateType() {
		return WASServer.class;
	}

	public static WASServer getWstDelegate(Object rspServerOrWorkingCopy) throws CoreException {
		return INSTANCE.getDelegate(rspServerOrWorkingCopy);
	}

	public static WASServer getWstDelegate(IServerAttributes server) throws CoreException {
		return server.getAdapter(WASServer.class);
	}

	public static IStatus validateWebSphereProfileExists(IServerAttributes server) throws CoreException {
		WASServer wstDelegate = getWstDelegate(server);
		if (wstDelegate == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
						"WST WASServer delegate not found"));
					}
		List<String> profiles = Arrays.asList(wstDelegate.getProfileNames());
		if (profiles == null || profiles.isEmpty() || !profiles.contains(wstDelegate.getProfileName())) {
			return new Status(IStatus.ERROR, Activator.BUNDLE_ID, "WebSphere profile '"
					+ wstDelegate.getProfileName() + "' does not exist in the specified WebSphere installation.");
		}
		return Status.OK_STATUS;
	}

	public static ServerXmlFileHandler createServerXmlFileHandler(Object rspServerOrWorkingCopy) throws IOException, CoreException  {
		WASServer server;
		server = getWstDelegate(rspServerOrWorkingCopy);
		return createServerXmlFileHandler(server.getWebSphereInstallPath(), server.getProfileName(), server.getBaseServerName());
	}

	public static ServerXmlFileHandler createServerXmlFileHandler(String curWASInstallRoot, String profileName, String serverName) throws IOException {
		return ServerXmlFileHandler.create(curWASInstallRoot, profileName, serverName);
	}

	public static int getDebugPortNum(Object rspServerOrWorkingCopy) throws CoreException {
		WASServer server = getWstDelegate(rspServerOrWorkingCopy);
		return server.getDebugPortNum();
	}

	public static int getDebugPort(Object rspServerOrWorkingCopy) {
		try {
			return createServerXmlFileHandler(rspServerOrWorkingCopy).getDebugPortNum();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String getWebSphereInstallPath(Object rspServerOrWorkingCopy) throws CoreException {
		return getWstDelegate(rspServerOrWorkingCopy).getWebSphereInstallPath();
	}

	public static String getProfileName(Object rspServerOrWorkingCopy) throws CoreException {
		return getWstDelegate(rspServerOrWorkingCopy).getProfileName();
	}

	public static String getBaseServerName(Object rspServerOrWorkingCopy) throws CoreException {
		return getWstDelegate(rspServerOrWorkingCopy).getBaseServerName();
	}

	public static String getServerXmlFilePath(Object rspServerOrWorkingCopy) throws IOException, CoreException {
		return createServerXmlFileHandler(rspServerOrWorkingCopy).getServerXMLFilePath();
	}

	public static void setSystemProperties(Object rspServerOrWorkingCopy, Map<String, String> systemProperties) throws CoreException {
		try {
			ServerXmlFileHandler handler = createServerXmlFileHandler(rspServerOrWorkingCopy);
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

	public static Map<String, String> getSystemProperties(Object rspServerOrWorkingCopy) throws CoreException {
		try {
			IMemento jvmEntry = createServerXmlFileHandler(rspServerOrWorkingCopy).getJavaVirtualMachine();
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
}
