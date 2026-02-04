package com.github.cabutchei.rsp.api;

/**
 * Combined client interface that exposes both RSP and WTP client methods
 * over a single JSON-RPC connection.
 */
public interface RSPWTPClient extends RSPClient, WTPClient {}
