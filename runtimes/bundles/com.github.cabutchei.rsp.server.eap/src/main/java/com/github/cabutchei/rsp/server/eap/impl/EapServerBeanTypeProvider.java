package com.github.cabutchei.rsp.server.eap.impl;

import com.github.cabutchei.rsp.server.eap.beans.EapServerBeanTypeEAP70;
import com.github.cabutchei.rsp.server.spi.discovery.IServerBeanTypeProvider;
import com.github.cabutchei.rsp.server.spi.discovery.ServerBeanType;

public class EapServerBeanTypeProvider implements IServerBeanTypeProvider {

    private static final ServerBeanType[] TYPES = {
            new EapServerBeanTypeEAP70()
    };

    @Override
    public ServerBeanType[] getServerBeanTypes() {
        return TYPES;
    }
}
