package com.github.cabutchei.rsp.api;

/**
 * Combined server interface that exposes both RSPServer and WTPServer methods
 * over a single JSON-RPC connection.
 */
public interface RSPWTPServer extends RSPServer, WTPServer {
	// marker interface
}
