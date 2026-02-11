package com.github.cabutchei.rsp.server.eap.beans;

import java.io.File;

public abstract class EapServerBeanTypeEnterprise extends EapServerBeanType {

    protected EapServerBeanTypeEnterprise(String id, String name, String systemJarPath) {
        super(id, name, systemJarPath);
    }

    protected String getServerTypeBaseName() {
        return "Red Hat JBoss " + getId();
    }

    @Override
    public String getFullVersion(File root) {
        return getFullVersion(root, new File(root, systemJarPath));
    }
}
