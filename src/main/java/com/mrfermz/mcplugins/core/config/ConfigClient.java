package com.mrfermz.mcplugins.core.config;

import java.util.Optional;

/**
 * Read access to values managed by the external {@code webconfig/} service.
 *
 * <p>Plugins read settings through this client, which caches values in memory
 * and refreshes them periodically (or via webhook/pub-sub) rather than hitting
 * the database on every lookup (see CLAUDE.md → Web-config).
 *
 * <p><b>Status:</b> interface placeholder; the concrete REST/poll client is
 * implemented alongside the {@code webconfig/} service.
 */
public interface ConfigClient {

    Optional<String> getString(String key);

    Optional<Boolean> getBoolean(String key);

    Optional<Long> getLong(String key);
}
