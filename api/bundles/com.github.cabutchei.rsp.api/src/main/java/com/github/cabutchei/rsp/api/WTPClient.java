package com.github.cabutchei.rsp.api;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import com.github.cabutchei.rsp.api.dao.JreContainerMappings;
import com.github.cabutchei.rsp.api.dao.ClasspathContainerMappings;



@JsonSegment("wtpClient")
public interface WTPClient {
    
	/**
	 * The `client/jdtlsJreContainersDetected` notification is sent by the server
	 * when non-standard JRE containers are detected and resolved to VM installs.
	 */
	@JsonNotification
	void jdtlsJreContainersDetected(JreContainerMappings mappings);

	/**
	 * The `client/jdtlsClasspathContainersDetected` notification is sent by the server
	 * when non-JRE classpath containers are detected and resolved to entries.
	 */
	@JsonNotification
	void jdtlsClasspathContainersDetected(ClasspathContainerMappings mappings);
}
