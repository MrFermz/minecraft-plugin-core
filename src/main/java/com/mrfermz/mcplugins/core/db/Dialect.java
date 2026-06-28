package com.mrfermz.mcplugins.core.db;

import java.util.Locale;

/**
 * The SQL dialect of the configured {@link DatabaseService}, so consumers can
 * pick engine-correct SQL (UPSERT syntax, column types, …) without sniffing
 * connection metadata.
 */
public enum Dialect {

    SQLITE,
    POSTGRESQL,
    MYSQL,
    MARIADB;

    /** MySQL and MariaDB share SQL syntax (e.g. {@code INSERT ... ON DUPLICATE KEY}). */
    public boolean isMySqlFamily() {
        return this == MYSQL || this == MARIADB;
    }

    /** Parses the {@code database.type} config value; unknown values fall back to SQLite. */
    public static Dialect fromType(String type) {
        if (type == null) {
            return SQLITE;
        }
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "postgres", "postgresql" -> POSTGRESQL;
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            default -> SQLITE;
        };
    }
}
