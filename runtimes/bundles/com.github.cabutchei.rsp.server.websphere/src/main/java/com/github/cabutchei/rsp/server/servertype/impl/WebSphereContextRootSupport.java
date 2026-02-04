package com.github.cabutchei.rsp.server.servertype.impl;

import com.github.cabutchei.rsp.api.dao.DeployableState;
import com.github.cabutchei.rsp.server.generic.jee.ContextRootSupport;

public class WebSphereContextRootSupport extends ContextRootSupport {

	public String[] getDeploymentUrls(String strat, String baseUrl, 
			String deployableOutputName, DeployableState ds) {
		if( deployableOutputName.equalsIgnoreCase("root.war") || deployableOutputName.equalsIgnoreCase("root")) {
			return new String[] {baseUrl};
		}
		String withSlashes = deployableOutputName.replaceAll("#", "/");
		String ret = baseUrl;
		if( !baseUrl.endsWith("/"))
			ret += "/";
		ret += removeWarSuffix(withSlashes);
		return new String[] { ret };
		
	}
	@Override
	protected String[] getCustomWebDescriptorsRelativePath() {
		return new String[] { };
	}

	@Override
	protected String findFromWebDescriptorString(String descriptorContents) {
		// TODO Auto-generated method stub
		return null;
	}

}
