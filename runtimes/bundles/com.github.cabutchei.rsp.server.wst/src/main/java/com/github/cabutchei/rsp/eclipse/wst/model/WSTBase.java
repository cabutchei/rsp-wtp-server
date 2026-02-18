package com.github.cabutchei.rsp.eclipse.wst.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.cabutchei.rsp.launching.memento.IMemento;
import com.github.cabutchei.rsp.launching.memento.JSONMemento;

class WSTBase {
	private static final String MAP_PROPERTIES_KEY = "mapProperties";
	private static final String LIST_PROPERTIES_KEY = "listProperties";
	private static final String MAP_PROPERTY_KEY_PREFIX = "mapProperty";
	private static final String LIST_PROPERTY_KEY_PREFIX = "listProperty";
	private static final String PROPERTY_KEY_VALUE_PREFIX = "value";

	protected Map<String, Object> map = new HashMap<>();

	public String getAttribute(String attributeName, String defaultValue) {
		try {
			Object obj = map.get(attributeName);
			if (obj == null) {
				return defaultValue;
			}
			return (String) obj;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public int getAttribute(String attributeName, int defaultValue) {
		try {
			Object obj = map.get(attributeName);
			if (obj == null) {
				return defaultValue;
			}
			return Integer.parseInt((String) obj);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public boolean getAttribute(String attributeName, boolean defaultValue) {
		try {
			Object obj = map.get(attributeName);
			if (obj == null) {
				return defaultValue;
			}
			return Boolean.parseBoolean((String) obj);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> getAttribute(String attributeName, List<String> defaultValue) {
		try {
			Object obj = map.get(attributeName);
			if (obj == null) {
				return defaultValue;
			}
			return (List<String>) obj;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@SuppressWarnings("unchecked")
	public Map getAttribute(String attributeName, Map defaultValue) {
		try {
			Object obj = map.get(attributeName);
			if (obj == null) {
				return defaultValue;
			}
			return (Map) obj;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	protected IMemento loadMemento(InputStream in) throws IOException {
		return JSONMemento.loadMemento(in);
	}

	protected void load(IMemento memento) {
		map = new HashMap<>();
		Iterator<String> iterator = memento.getNames().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			map.put(key, memento.getString(key));
		}

		IMemento[] allChildren = memento.getChildren();
		for (int i = 0; i < allChildren.length; i++) {
			if (allChildren[i].getNodeName().startsWith(LIST_PROPERTY_KEY_PREFIX)) {
				map.put(allChildren[i].getNodeName(), getListFromMemento(allChildren[i]));
			}
			if (allChildren[i].getNodeName().startsWith(MAP_PROPERTY_KEY_PREFIX)) {
				map.put(allChildren[i].getNodeName(), getMapFromMemento(allChildren[i]));
			}
		}

		IMemento[] children = memento.getChildren(LIST_PROPERTIES_KEY);
		if (children != null) {
			for (IMemento child : children) {
				loadList(child);
			}
		}
		IMemento[] maps = memento.getChildren(MAP_PROPERTIES_KEY);
		if (maps != null) {
			for (IMemento m : maps) {
				loadMap(m);
			}
		}
	}

	private void loadMap(IMemento memento) {
		IMemento[] kids = memento.getChildren();
		if (kids != null) {
			for (int i = 0; i < kids.length; i++) {
				String name = kids[i].getNodeName();
				map.put(name, getMapFromMemento(kids[i]));
			}
		}
	}

	private Map<String, String> getMapFromMemento(IMemento memento) {
		Map<String, String> vMap = new HashMap<>();
		Iterator<String> iterator = memento.getNames().iterator();
		while (iterator.hasNext()) {
			String s = iterator.next();
			String v = memento.getString(s);
			vMap.put(s, v);
		}
		return vMap;
	}

	private void loadList(IMemento memento) {
		IMemento[] kids = memento.getChildren();
		if (kids != null) {
			for (int i = 0; i < kids.length; i++) {
				String name = kids[i].getNodeName();
				map.put(name, getListFromMemento(kids[i]));
			}
		}
	}

	private List<String> getListFromMemento(IMemento memento) {
		List<String> list = new ArrayList<>();
		int i = 0;
		String key2 = memento.getString(PROPERTY_KEY_VALUE_PREFIX + (i++));
		while (key2 != null) {
			list.add(key2);
			key2 = memento.getString(PROPERTY_KEY_VALUE_PREFIX + (i++));
		}
		return list;
	}

}
