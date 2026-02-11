package com.github.cabutchei.rsp.server.eap.beans;

import java.io.File;
import java.util.Properties;

import com.github.cabutchei.rsp.eclipse.core.runtime.IPath;
import com.github.cabutchei.rsp.eclipse.core.runtime.Path;
import com.github.cabutchei.rsp.launching.utils.FileUtil;
import com.github.cabutchei.rsp.server.spi.discovery.ServerBeanType;

public abstract class EapServerBeanType extends ServerBeanType implements IEapServerResourceConstants {

    protected final String systemJarPath;

    protected EapServerBeanType(String id, String name, String systemJarPath) {
        super(id, name);
        this.systemJarPath = systemJarPath;
    }

    @Override
    public String getUnderlyingTypeId(File root) {
        return getId();
    }

    @Override
    public String getFullVersion(File root) {
        return getFullVersion(root, new File(root, systemJarPath));
    }

    protected String getFullVersion(File root, File systemJar) {
        return null;
    }

    protected String getEap6xVersion(File location, String metaInfPath,
            String versionPrefix, String slot, String releaseName) {
        IPath productConf = new Path(location.getAbsolutePath()).append(BIN).append(PRODUCT_CONF);
        if (productConf.toFile().exists()) {
            Properties props = FileUtil.loadProperties(productConf.toFile());
            String product = (String) props.get(PRODUCT_CONF_SLOT);
            if (slot.equals(product)) {
                return getEap6xVersionNoSlotCheck(location, metaInfPath, versionPrefix, releaseName);
            }
        }
        return null;
    }

    protected String getEap6xVersionNoSlotCheck(File location, String metaInfPath,
            String versionPrefix, String releaseName) {
        IPath rootPath = new Path(location.getAbsolutePath());
        IPath eapDir = rootPath.append(metaInfPath);
        if (eapDir.toFile().exists()) {
            IPath manifest = eapDir.append(MANIFEST_MF);
            Properties props = FileUtil.loadProperties(manifest.toFile());
            String name = props.getProperty(MANIFEST_PROD_RELEASE_NAME);
            String version = props.getProperty(MANIFEST_PROD_RELEASE_VERS);
            boolean matchesName = releaseName == null || releaseName.equals(name);
            boolean matchesVersion = versionPrefix == null || version.startsWith(versionPrefix);
            if (matchesName && matchesVersion) {
                return version;
            }
        }
        return null;
    }
}
