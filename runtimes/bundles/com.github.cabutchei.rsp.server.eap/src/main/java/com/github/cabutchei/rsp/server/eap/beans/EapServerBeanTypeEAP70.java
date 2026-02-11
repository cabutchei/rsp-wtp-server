package com.github.cabutchei.rsp.server.eap.beans;

import java.io.File;

import com.github.cabutchei.rsp.server.eap.servertype.IEapServerAttributes;

public class EapServerBeanTypeEAP70 extends EapServerBeanTypeEnterprise {

    public EapServerBeanTypeEAP70() {
        super(ID_EAP, NAME_EAP, AS7_MODULE_LAYERED_SERVER_MAIN);
    }

    @Override
    public String getServerAdapterTypeId(String version) {
        return IEapServerAttributes.EAP_70_SERVER_TYPE;
    }

    @Override
    public boolean isServerRoot(File location) {
        return getEap6xVersion(location, EAP_LAYERED_PRODUCT_META_INF, "7.0", SLOT_EAP, RELEASE_NAME_JBOSS_EAP) != null;
    }

    @Override
    public String getFullVersion(File location) {
        return getEap6xVersion(location, EAP_LAYERED_PRODUCT_META_INF, "7.0", SLOT_EAP, RELEASE_NAME_JBOSS_EAP);
    }
}
