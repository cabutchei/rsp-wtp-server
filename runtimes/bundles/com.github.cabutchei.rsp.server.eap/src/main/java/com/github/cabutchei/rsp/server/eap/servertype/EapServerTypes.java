package com.github.cabutchei.rsp.server.eap.servertype;

public final class EapServerTypes {
	public static final String EAP_70_ID = IEapServerAttributes.EAP_70_SERVER_TYPE;
	public static final String EAP_70_NAME = "JBoss EAP 7.0";
	public static final String EAP_70_DESC = "JBoss Enterprise Application Platform 7.0";

	public static final EapServerType EAP_70_SERVER_TYPE =
			new EapServerType(EAP_70_ID, EAP_70_NAME, EAP_70_DESC);

	private EapServerTypes() {
		// no-op
	}
}
