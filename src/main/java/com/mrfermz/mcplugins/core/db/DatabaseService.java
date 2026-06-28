package com.mrfermz.mcplugins.core.db;

import javax.sql.DataSource;

/**
 * The single shared database access point for the ecosystem.
 *
 * <p>Core owns exactly one connection pool (HikariCP) and one migration runner;
 * no other plugin may open its own pool (see CLAUDE.md → Database). Plugins get
 * the {@link DataSource} from here and keep their tables namespaced with a
 * per-plugin prefix (e.g. {@code money_}).
 *
 * <p><b>Status:</b> wired — core registers a {@link HikariDatabaseService} on
 * enable, configured from {@code database.*} in the global {@code config.yml}.
 * SQLite (embedded) is the default; PostgreSQL is the recommended production
 * engine, with MySQL/MariaDB also supported. Look it up via
 * {@link com.mrfermz.mcplugins.core.CoreApi#database}. Use {@link #dialect()} to
 * emit engine-correct SQL.
 */
public interface DatabaseService {

    /** The shared pooled {@link DataSource}. */
    DataSource dataSource();

    /** The SQL dialect, so callers can emit engine-correct SQL. */
    Dialect dialect();

    /** Table-name prefix reserved for the calling plugin, e.g. {@code "money_"}. */
    String tablePrefix(String pluginNamespace);
}
