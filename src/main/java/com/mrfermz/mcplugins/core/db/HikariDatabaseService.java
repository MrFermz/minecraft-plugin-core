package com.mrfermz.mcplugins.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;

/**
 * {@link DatabaseService} backed by a HikariCP pool — the one and only
 * connection pool in the ecosystem (CLAUDE.md → Database).
 *
 * <p>Builds the JDBC URL and driver from {@link DatabaseSettings} for any
 * supported engine:
 * <ul>
 *   <li>{@link Dialect#SQLITE} — embedded file, zero-config (default)</li>
 *   <li>{@link Dialect#POSTGRESQL} — recommended for production</li>
 *   <li>{@link Dialect#MYSQL} / {@link Dialect#MARIADB} — via the MariaDB driver</li>
 * </ul>
 *
 * <p>Drivers are loaded by class name at runtime (Paper's library loader), so
 * only the configured engine's driver actually needs to resolve.
 */
public final class HikariDatabaseService implements DatabaseService, AutoCloseable {

    private final HikariDataSource dataSource;
    private final Dialect dialect;

    public HikariDatabaseService(DatabaseSettings settings) {
        this.dialect = settings.dialect();

        HikariConfig config = new HikariConfig();
        config.setPoolName("antitle-db");

        switch (dialect) {
            case SQLITE -> {
                config.setJdbcUrl("jdbc:sqlite:" + settings.sqliteFile().getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
                // SQLite is single-writer; one connection avoids lock churn.
                config.setMaximumPoolSize(1);
                config.setConnectionInitSql("PRAGMA busy_timeout = 5000");
                // Store java.sql.Timestamp as readable ISO text, not epoch millis,
                // so date columns (e.g. money_transactions.created_at) are real dates.
                config.addDataSourceProperty("date_class", "text");
            }
            case POSTGRESQL -> {
                config.setJdbcUrl("jdbc:postgresql://" + settings.host() + ":" + settings.port()
                        + "/" + settings.database() + query(settings.properties()));
                config.setDriverClassName("org.postgresql.Driver");
                applyCredentials(config, settings);
            }
            case MYSQL, MARIADB -> {
                // The MariaDB Connector/J driver also connects to MySQL servers.
                config.setJdbcUrl("jdbc:mariadb://" + settings.host() + ":" + settings.port()
                        + "/" + settings.database() + query(settings.properties()));
                config.setDriverClassName("org.mariadb.jdbc.Driver");
                applyCredentials(config, settings);
            }
        }

        this.dataSource = new HikariDataSource(config);
    }

    private static void applyCredentials(HikariConfig config, DatabaseSettings settings) {
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(Math.max(1, settings.poolSize()));
    }

    /** Renders extra params as a {@code ?k=v&k2=v2} query string (empty if none). */
    private static String query(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> e : properties.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(enc(e.getKey())).append('=').append(enc(String.valueOf(e.getValue())));
            first = false;
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public Dialect dialect() {
        return dialect;
    }

    @Override
    public String tablePrefix(String pluginNamespace) {
        return pluginNamespace.toLowerCase(Locale.ROOT) + "_";
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
