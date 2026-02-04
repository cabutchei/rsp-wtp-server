package com.github.cabutchei.rsp.server.spi.servertype;

import java.util.List;

import com.github.cabutchei.rsp.api.dao.ModuleState;

/**
 * Optional capability for server delegates that can provide module state information.
 */
public interface IModuleStateProvider {
	List<ModuleState> getModuleStates();
}
