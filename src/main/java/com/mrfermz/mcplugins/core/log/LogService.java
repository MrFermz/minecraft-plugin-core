package com.mrfermz.mcplugins.core.log;

/**
 * The single, centralized log sink for the whole ecosystem.
 *
 * <p>Core owns one {@link LogService} and registers it on Bukkit's
 * {@code ServicesManager}, mirroring how it owns the one
 * {@link com.mrfermz.mcplugins.core.db.DatabaseService database pool}. Every
 * plugin already logs through {@link PluginLog}, which forwards each line here,
 * so all operational logs land in the same place (file + central DB) with a
 * consistent format — no feature plugin has to wire up its own persistence.
 *
 * <p>Implementations buffer and persist asynchronously: {@link #log} never
 * blocks the caller, so it is safe to call from the main server thread. Look it
 * up via {@link com.mrfermz.mcplugins.core.CoreApi#logging}.
 */
public interface LogService {

    /** Records a log line (fast, buffered — no blocking I/O on the caller). */
    void log(LogEntry entry);

    /** Persists any buffered lines to the configured sinks. */
    void flush();
}
