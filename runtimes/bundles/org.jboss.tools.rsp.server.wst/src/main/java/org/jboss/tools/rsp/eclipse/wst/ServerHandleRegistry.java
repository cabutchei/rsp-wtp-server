package org.jboss.tools.rsp.eclipse.wst;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// RSP type import will be whatever your domain uses.

public class ServerHandleRegistry {

    private final ConcurrentMap<String, org.jboss.tools.rsp.server.spi.servertype.IServer> rspById = new ConcurrentHashMap<>();

    /**
     * Register or refresh the WST server and (optionally) its RSP projection.
     * WST is always the source of truth.
     */

    public void register(org.jboss.tools.rsp.server.spi.servertype.IServer rsp) {
        rspById.put(rsp.getId(), rsp);
    }

    public org.jboss.tools.rsp.server.spi.servertype.IServer getRsp(String id) {
        return rspById.get(id);
    }

    /**
     * Lazily project an RSP server from WST if needed.
     */
    // public org.jboss.tools.rsp.server.spi.servertype.IServer getOrCreateRsp(org.eclipse.wst.server.core.IServer wst, Function<org.eclipse.wst.server.core.IServer, org.jboss.tools.rsp.server.spi.servertype.IServer> factory) {
    //     Objects.requireNonNull(wst, "wst");
    //     Objects.requireNonNull(factory, "factory");
    //     String id = wst.getId();
    //     wstById.put(id, wst);
    //     return rspById.computeIfAbsent(id, k -> factory.apply(wst));
    // }

    public Map<String, org.jboss.tools.rsp.server.spi.servertype.IServer> getAllRspServers() {
        return rspById;
    }

    public void unregister(String id) {
        rspById.remove(id);
    }

    public void clear() {
        rspById.clear();
    }
}
