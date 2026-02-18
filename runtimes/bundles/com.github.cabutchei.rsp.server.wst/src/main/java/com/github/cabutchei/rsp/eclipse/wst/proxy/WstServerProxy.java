package com.github.cabutchei.rsp.eclipse.wst.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import com.github.cabutchei.rsp.api.ServerManagementAPIConstants;
import com.github.cabutchei.rsp.api.dao.Attributes;
import com.github.cabutchei.rsp.api.dao.util.CreateServerAttributesUtility;
import com.github.cabutchei.rsp.eclipse.core.runtime.CoreException;
import com.github.cabutchei.rsp.eclipse.core.runtime.IProgressMonitor;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.eclipse.wst.adapter.WstModelAdapter;
import com.github.cabutchei.rsp.eclipse.wst.api.IWstServerBacked;
import com.github.cabutchei.rsp.launching.memento.IMemento;
import com.github.cabutchei.rsp.launching.memento.JSONMemento;
import com.github.cabutchei.rsp.server.ServerCoreActivator;
import com.github.cabutchei.rsp.server.spi.model.IServerManagementModel;
import com.github.cabutchei.rsp.server.spi.model.IServerModel;
import com.github.cabutchei.rsp.server.spi.servertype.IServer;
import com.github.cabutchei.rsp.server.spi.servertype.IServerDelegate;
import com.github.cabutchei.rsp.server.spi.servertype.IServerType;
import com.github.cabutchei.rsp.server.spi.servertype.IServerWorkingCopy;



/** A proxy for a WST server that implements the IServer interface. */

public class WstServerProxy implements IServer, IWstServerBacked {
	// TODO: do we need these properties?
	public static final String TYPE_ID = "server-type-id";
	private static final String MAP_PROPERTIES_KEY = "mapProperties";
	private static final String LIST_PROPERTIES_KEY = "listProperties";
	private static final String MAP_PROPERTY_KEY_PREFIX = "mapProperty";
	private static final String LIST_PROPERTY_KEY_PREFIX = "listProperty";
	private static final String PROPERTY_KEY_VALUE_PREFIX = "value";

	private final org.eclipse.wst.server.core.IServer wstServer;
	private final IServerManagementModel managementModel;
	private final IServerModel serverModel;
	private IServerDelegate delegate;

	public WstServerProxy(org.eclipse.wst.server.core.IServer wstServer, IServerManagementModel managementModel) {
		this.wstServer = Objects.requireNonNull(wstServer, "wstServer");
		this.managementModel = managementModel;
		this.serverModel = managementModel.getServerModel();
		if(getServerType() != null ) {
			// setAttribute(TYPE_ID, serverType.getId());
			this.delegate = getServerType().createServerDelegate(this);
		}
		if( delegate != null && delegate.getServerPublishModel() != null ) {
			delegate.getServerPublishModel().initialize(Collections.emptyList());
		}
		// if( this.delegate != null ) {
		// 	this.delegate.setDefaults(this);
		// }
		// setAttributes(attributes);
		// if( this.delegate != null ) {
		// 	this.delegate.setDependentDefaults(this);
		// }
	}

	@Override
	public org.eclipse.wst.server.core.IServer getWstServer() {
		return wstServer;
	}

	public void setDelegate(IServerDelegate delegate) {
		this.delegate = delegate;
		if( delegate != null && delegate.getServerPublishModel() != null )
			delegate.getServerPublishModel().initialize(Collections.EMPTY_LIST);
	}

	@Override
	public String getName() {
		return wstServer.getName();
	}

	@Override
	public String getId() {
		return wstServer.getId();
	}

	@Override
	public String getTypeId() {
		if (getServerType() != null) {
			return getServerType().getId();
		}
		if (wstServer.getServerType() != null) {
			return wstServer.getServerType().getId();
		}
		return null;
	}

	@Override
	public IServerType getServerType() {
		return WstModelAdapter.toRspServerType(wstServer.getServerType(), serverModel);
	}

	public void setServerType(IServerType type) {
	}

	@Override
	public IServerDelegate getDelegate() {
		return delegate;
	}

	@Override
	public IServerWorkingCopy createWorkingCopy() {
		org.eclipse.wst.server.core.IServerWorkingCopy copy = wstServer.createWorkingCopy();
		return new WstServerWorkingCopyProxy(copy, managementModel);
	}

	@Override
	public <T> T getAdapter(Class<T> adapterType) {
		if (adapterType == null) {
			return null;
		}
		if (adapterType.isInstance(this)) {
			return adapterType.cast(this);
		}
			if (adapterType.isInstance(wstServer)) {
				return adapterType.cast(wstServer);
			}
			try {
				Object adapted = wstServer.loadAdapter(adapterType, null);
				return adapterType.cast(adapted);
			} catch (Exception e) {
				return null;
			}
		}

	@Override
	public String asJson(IProgressMonitor monitor) throws CoreException {
		JSONMemento memento = JSONMemento.createWriteRoot();
		Map<String, Object> attributes = buildAttributeMap();
		saveAttributes(memento, attributes);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			memento.save(out);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID,
					"Could not save server to stream", e));
		}
		return new String(out.toByteArray());
	}

	@Override
	public void load(IProgressMonitor monitor) throws CoreException {
		// No-op: WST is the source of truth.
	}

	@Override
	public void delete() throws CoreException {
		try {
			wstServer.delete();
		} catch (org.eclipse.core.runtime.CoreException ce) {
			throw new CoreException(WstModelAdapter.toRspStatus(ce.getStatus()));
		}
	}

	@Override
	public IServerManagementModel getServerManagementModel() {
		return managementModel;
	}

	@Override
	public IServerModel getServerModel() {
		return serverModel;
	}

	@Override
	public int getAttribute(String attributeName, int defaultValue) {
		return wstServer.getAttribute(attributeName, defaultValue);
	}

	@Override
	public boolean getAttribute(String attributeName, boolean defaultValue) {
		return wstServer.getAttribute(attributeName, defaultValue);
	}

	@Override
	public String getAttribute(String attributeName, String defaultValue) {
		return wstServer.getAttribute(attributeName, defaultValue);
	}

	@Override
	public List<String> getAttribute(String attributeName, List<String> defaultValue) {
		return wstServer.getAttribute(attributeName, defaultValue);
	}

	@Override
	public Map getAttribute(String attributeName, Map defaultValue) {
		return wstServer.getAttribute(attributeName, defaultValue);
	}

	private Map<String, Object> buildAttributeMap() {
		Map<String, Object> map = new HashMap<>();
		String id = wstServer.getId();
		map.put("id", id);
		map.put("id-set", Boolean.toString(true));
		if (wstServer.getName() != null) {
			map.put("name", wstServer.getName());
		}
		String typeId = getTypeId();
		if (typeId != null) {
			map.put(TYPE_ID, typeId);
		}
		if (getServerType() != null) {
			addAttributesFromType(map, getServerType().getRequiredAttributes());
			addAttributesFromType(map, getServerType().getOptionalAttributes());
		}
		return map;
	}

	private void addAttributesFromType(Map<String, Object> map, Attributes attrs) {
		if (attrs == null) {
			return;
		}
		CreateServerAttributesUtility util = new CreateServerAttributesUtility(attrs);
		Set<String> keys = util.listAttributes();
		for (String key : keys) {
			if (map.containsKey(key)) {
				continue;
			}
			String type = util.getAttributeType(key);
			Object defaultVal = util.getAttributeDefaultValue(key);
			Object value = readAttribute(key, type, defaultVal);
			if (value != null) {
				map.put(key, value);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Object readAttribute(String key, String type, Object defaultVal) {
		if (ServerManagementAPIConstants.ATTR_TYPE_INT.equals(type)) {
			int def = defaultVal instanceof Integer ? ((Integer) defaultVal).intValue() : 0;
			return Integer.valueOf(wstServer.getAttribute(key, def));
		}
		if (ServerManagementAPIConstants.ATTR_TYPE_BOOL.equals(type)) {
			boolean def = defaultVal instanceof Boolean ? ((Boolean) defaultVal).booleanValue() : false;
			return Boolean.valueOf(wstServer.getAttribute(key, def));
		}
		if (ServerManagementAPIConstants.ATTR_TYPE_LIST.equals(type)) {
			List<String> def = defaultVal instanceof List ? (List<String>) defaultVal : Collections.emptyList();
			return wstServer.getAttribute(key, def);
		}
		if (ServerManagementAPIConstants.ATTR_TYPE_MAP.equals(type)) {
			Map<String, String> def = defaultVal instanceof Map ? (Map<String, String>) defaultVal : Collections.emptyMap();
			return wstServer.getAttribute(key, def);
		}
		String def = defaultVal instanceof String ? (String) defaultVal : null;
		return wstServer.getAttribute(key, def);
	}

	private void saveAttributes(IMemento memento, Map<String, Object> map) {
		Set<String> keys = map.keySet();
		ArrayList<String> keyList = new ArrayList<>(keys);
		Collections.sort(keyList);
		Iterator<String> iterator = keyList.iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			Object obj = map.get(key);
			if (obj instanceof String) {
				memento.putString(key, (String) obj);
			} else if (obj instanceof Integer) {
				memento.putInteger(key, ((Integer) obj).intValue());
			} else if (obj instanceof Boolean) {
				memento.putBoolean(key, ((Boolean) obj).booleanValue());
			} else if (obj instanceof List) {
				List<String> list = (List<String>) obj;
				saveList(memento, key, list);
			} else if (obj instanceof Map) {
				Map<String, String> map2 = (Map<String, String>) obj;
				saveMap(memento, key, map2);
			}
		}
	}

	private void saveMap(IMemento memento, String key, Map<String, String> map2) {
		IMemento toUse = null;
		if (key.startsWith(MAP_PROPERTY_KEY_PREFIX)) {
			toUse = memento;
		} else {
			toUse = memento.getChild(MAP_PROPERTIES_KEY);
			if (toUse == null) {
				toUse = memento.createChild(MAP_PROPERTIES_KEY);
			}
		}

		IMemento keyChild = toUse.createChild(key);
		Iterator<String> iterator = map2.keySet().iterator();
		while (iterator.hasNext()) {
			String s = iterator.next();
			keyChild.putString(s, map2.get(s));
		}
	}

	private void saveList(IMemento memento, String key, List<String> list) {
		IMemento toUse = null;
		if (key.startsWith(LIST_PROPERTY_KEY_PREFIX)) {
			toUse = memento;
		} else {
			toUse = memento.getChild(LIST_PROPERTIES_KEY);
			if (toUse == null) {
				toUse = memento.createChild(LIST_PROPERTIES_KEY);
			}
		}

		IMemento keyChild = toUse.createChild(key);
		int i = 0;
		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			String s = iterator.next();
			keyChild.putString(PROPERTY_KEY_VALUE_PREFIX + (i++), s);
		}
	}

	public void addServerListener(com.github.cabutchei.rsp.server.spi.servertype.IServerListener listener) {
		org.eclipse.wst.server.core.IServerListener wrapper = new org.eclipse.wst.server.core.IServerListener() {
			public void serverChanged(org.eclipse.wst.server.core.ServerEvent event) {
				com.github.cabutchei.rsp.server.spi.servertype.ServerEvent rspEvent = WstModelAdapter.toRspServerEvent(event,
						WstServerProxy.this);
				listener.serverChanged(rspEvent);
			}
		};
		this.wstServer.addServerListener(wrapper);
	}

}
