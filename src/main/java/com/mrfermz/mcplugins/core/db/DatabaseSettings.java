package com.mrfermz.mcplugins.core.db;

import java.io.File;
import java.util.Map;

/**
 * Connection settings for the shared database, read from core's global
 * {@code config.yml} ({@code database.*}). One value object covers every engine:
 * {@link #sqliteFile} is used for SQLite, the network fields for the rest.
 *
 * @param dialect    which engine / SQL dialect
 * @param sqliteFile DB file for {@link Dialect#SQLITE} (ignored otherwise)
 * @param host       server host (network engines)
 * @param port       server port (network engines)
 * @param database   database/schema name (network engines)
 * @param username   login user (network engines)
 * @param password   login password (network engines)
 * @param properties extra JDBC params appended to the URL (e.g. {@code sslmode=require})
 * @param poolSize   HikariCP max pool size (forced to 1 for SQLite)
 */
public record DatabaseSettings(
        Dialect dialect,
        File sqliteFile,
        String host,
        int port,
        String database,
        String username,
        String password,
        Map<String, String> properties,
        int poolSize) {
}
