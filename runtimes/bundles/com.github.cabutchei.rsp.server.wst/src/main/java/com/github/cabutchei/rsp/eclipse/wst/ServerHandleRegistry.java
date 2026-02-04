package com.github.cabutchei.rsp.eclipse.wst;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// RSP type import will be whatever our domain uses.

public class ServerHandleRegistry {

    private final ConcurrentMap<String, com.github.cabutchei.rsp.server.spi.servertype.IServer> rspById = new ConcurrentHashMap<>();

    /**
     * Register or refresh the WST server and (optionally) its RSP projection.
     * WST is always the source of truth.
     */

    public void register(com.github.cabutchei.rsp.server.spi.servertype.IServer rsp) {
        rspById.put(rsp.getId(), rsp);
    }

    public com.github.cabutchei.rsp.server.spi.servertype.IServer getRsp(String id) {
        return rspById.get(id);
    }

    public Map<String, com.github.cabutchei.rsp.server.spi.servertype.IServer> getAllRspServers() {
        return new ConcurrentHashMap<>(rspById);
    }

    public void unregister(String id) {
        rspById.remove(id);
    }

    public void clear() {
        rspById.clear();
    }
}
