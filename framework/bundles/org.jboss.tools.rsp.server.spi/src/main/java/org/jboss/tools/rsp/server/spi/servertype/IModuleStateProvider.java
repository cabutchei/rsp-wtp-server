package org.jboss.tools.rsp.server.spi.servertype;

import java.util.List;

import org.jboss.tools.rsp.api.dao.ModuleState;

/**
 * Optional capability for server delegates that can provide module state information.
 */
public interface IModuleStateProvider {
	List<ModuleState> getModuleStates();
}
