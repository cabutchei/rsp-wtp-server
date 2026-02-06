package com.github.cabutchei.rsp.server.spi.util;

/**
 * Domain-specific equivalent of Eclipse's {@code IAdaptable}.
 * <p>
 * Implementations may return adapters to non-domain types (for example, WST
 * server objects) without forcing the entire SPI layer to depend on Eclipse
 * runtime packages.
 * </p>
 */
public interface IRspAdaptable {
	default <T> T getAdapter(Class<T> adapterType) {
		return null;
	}
}

