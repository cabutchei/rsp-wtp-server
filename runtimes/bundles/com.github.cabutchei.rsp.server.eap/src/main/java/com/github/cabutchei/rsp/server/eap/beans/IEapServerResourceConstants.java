package com.github.cabutchei.rsp.server.eap.beans;

import com.github.cabutchei.rsp.launching.utils.FileUtil;

public interface IEapServerResourceConstants {
    String BIN = "bin";
    String PRODUCT_CONF = "product.conf";
    String PRODUCT_CONF_SLOT = "slot";
    String SLOT_EAP = "eap";

    String META_INF = "META-INF";
    String MANIFEST_MF = "MANIFEST.MF";
    String MANIFEST_PROD_RELEASE_NAME = "JBoss-Product-Release-Name";
    String MANIFEST_PROD_RELEASE_VERS = "JBoss-Product-Release-Version";

    String EAP_LAYERED_PRODUCT_META_INF =
            FileUtil.asPath("modules", "system", "layers", "base", "org", "jboss", "as", "product", "eap", "dir", META_INF);

    String AS7_MODULE_LAYERED_SERVER_MAIN =
            FileUtil.asPath("modules", "system", "layers", "base", "org", "jboss", "as", "server", "main");

    String ID_EAP = "EAP";
    String NAME_EAP = "Enterprise Application Platform";
    String RELEASE_NAME_JBOSS_EAP = "JBoss EAP";
}
