package com.mrfermz.mcplugins.core.db;

import com.mrfermz.mcplugins.core.log.PluginLog;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * First-run helper that creates the target database if it doesn't exist yet, so
 * a fresh install works without an admin manually running {@code CREATE DATABASE}.
 *
 * <p>For network engines it connects to a maintenance database with the same
 * credentials and issues a {@code CREATE DATABASE}. SQLite needs nothing (the
 * file is created on first connection).
 *
 * <p>Requires the configured user to have permission to create databases
 * (PostgreSQL: {@code CREATEDB}); if it can't, this logs a warning and lets the
 * normal connection attempt surface the error.
 */
public final class DatabaseBootstrap {

    /** Database/schema names must be a simple identifier to be created safely. */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private DatabaseBootstrap() {
    }

    public static void ensureDatabaseExists(DatabaseSettings settings, PluginLog log) {
        Dialect dialect = settings.dialect();
        if (dialect == Dialect.SQLITE) {
            return; // file is created automatically on first connect
        }
        String db = settings.database();
        if (db == null || !SAFE_IDENTIFIER.matcher(db).matches()) {
            log.warn("Skipping database auto-create: '{}' is not a simple identifier.", db);
            return;
        }

        String driverClass;
        String maintenanceUrl;
        switch (dialect) {
            case POSTGRESQL -> {
                driverClass = "org.postgresql.Driver";
                maintenanceUrl = "jdbc:postgresql://" + settings.host() + ":" + settings.port() + "/postgres";
            }
            case MYSQL, MARIADB -> {
                driverClass = "org.mariadb.jdbc.Driver";
                maintenanceUrl = "jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/";
            }
            default -> {
                return;
            }
        }

        try {
            Driver driver = (Driver) Class.forName(driverClass, true, DatabaseBootstrap.class.getClassLoader())
                    .getDeclaredConstructor().newInstance();
            Properties props = new Properties();
            if (settings.username() != null) {
                props.setProperty("user", settings.username());
            }
            if (settings.password() != null) {
                props.setProperty("password", settings.password());
            }
            settings.properties().forEach(props::setProperty);

            try (Connection conn = driver.connect(maintenanceUrl, props)) {
                if (conn == null) {
                    log.warn("Database auto-create: the driver rejected {}.", maintenanceUrl);
                    return;
                }
                if (dialect == Dialect.POSTGRESQL) {
                    createPostgres(conn, db, log);
                } else {
                    // MySQL/MariaDB support IF NOT EXISTS directly.
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + db + "`");
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not auto-create database '{}' ({}). Create it manually if the next "
                    + "connection attempt fails.", db, ex.getMessage());
        }
    }

    private static void createPostgres(Connection conn, String db, PluginLog log) throws Exception {
        // PostgreSQL has no CREATE DATABASE IF NOT EXISTS, so probe first.
        boolean exists;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + db + "'")) {
            exists = rs.next();
        }
        if (exists) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE \"" + db + "\"");
        }
        log.info("Created database \"{}\".", db);
    }
}
