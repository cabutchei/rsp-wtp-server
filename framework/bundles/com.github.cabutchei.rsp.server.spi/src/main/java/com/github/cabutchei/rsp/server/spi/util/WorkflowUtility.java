package com.github.cabutchei.rsp.server.spi.util;

import com.github.cabutchei.rsp.api.dao.WorkflowResponse;
import com.github.cabutchei.rsp.eclipse.core.runtime.IStatus;
import com.github.cabutchei.rsp.eclipse.core.runtime.Status;
import com.github.cabutchei.rsp.server.spi.SPIActivator;

public class WorkflowUtility {

	public static WorkflowResponse quickResponse(int sev, String msg, long reqId) {
		return quickResponse(sev, msg, reqId, null);
	}
	public static WorkflowResponse quickResponse(int sev, String msg, long reqId, Throwable t) {
		WorkflowResponse resp = new WorkflowResponse();
		IStatus istat = new Status(sev, SPIActivator.BUNDLE_ID, msg, t);
		resp.setStatus(StatusConverter.convert(istat));
		if( reqId > 0 )
			resp.setRequestId(reqId);
		return resp;
		
	}
}
