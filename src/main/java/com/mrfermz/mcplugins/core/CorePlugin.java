package com.mrfermz.mcplugins.core;

import com.mrfermz.mcplugins.core.db.DatabaseBootstrap;
import com.mrfermz.mcplugins.core.db.DatabaseService;
import com.mrfermz.mcplugins.core.db.DatabaseSettings;
import com.mrfermz.mcplugins.core.db.Dialect;
import com.mrfermz.mcplugins.core.db.HikariDatabaseService;
import com.mrfermz.mcplugins.core.log.DbLogSink;
import com.mrfermz.mcplugins.core.log.DefaultLogService;
import com.mrfermz.mcplugins.core.log.FileLogSink;
import com.mrfermz.mcplugins.core.log.LogLevel;
import com.mrfermz.mcplugins.core.log.LogService;
import com.mrfermz.mcplugins.core.log.PluginLog;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the shared core plugin.
 *
 * <p>Core is loaded as its own plugin on the server (not shaded into each
 * plugin), so the shared database pool and web-config client exist exactly
 * once. Feature plugins declare {@code depend: [Core]} and reach
 * core's services through {@link CoreApi} / Bukkit's {@code ServicesManager}.
 *
 * <p>This class deliberately holds no game logic — only bootstrapping for the
 * shared infrastructure.
 */
public final class CorePlugin extends JavaPlugin {

    /** Safety-net flush of buffered log lines, in case the debounced flush stalls. */
    private static final long LOG_FLUSH_INTERVAL_MS = 1000L * 30; // 30 seconds

    private PluginLog log;
    private HikariDatabaseService database;
    private DefaultLogService logging;

    @Override
    public void onEnable() {
        this.log = PluginLog.of(this);
        // Global ecosystem settings live in plugins/antitle/config.yml.
        FileConfiguration config = EcosystemData.config(this);

        // Stand up centralized logging first (file sink), so the database
        // bootstrap below — and everything after — is persisted too.
        startLogging(config);
        startDatabase(config);
        // The DB sink can only attach once the pool is up.
        attachDbLogSink(config);

        log.info("minecraft-plugin-core enabled (shared API ready).");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            getServer().getServicesManager().unregister(DatabaseService.class, database);
            database.close();
            log.info("Central database closed.");
        }
        if (logging != null) {
            log.info("minecraft-plugin-core disabled.");
            getServer().getServicesManager().unregister(LogService.class, logging);
            logging.close(); // flush remaining lines and close file handles
        } else if (log != null) {
            log.info("minecraft-plugin-core disabled.");
        }
    }

    private void startLogging(FileConfiguration config) {
        LogLevel level = LogLevel.fromConfig(config.getString("logging.level", "info"));
        this.logging = new DefaultLogService(this, level);

        if (config.getBoolean("logging.file.enabled", true)) {
            logging.addSink(new FileLogSink(EcosystemData.folder(this, "logs"), getLogger()));
        }

        getServer().getServicesManager().register(
                LogService.class, logging, this, ServicePriority.Normal);

        // Periodic safety-net flush off the main thread.
        getServer().getAsyncScheduler().runAtFixedRate(this,
                task -> logging.flush(),
                LOG_FLUSH_INTERVAL_MS, LOG_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void attachDbLogSink(FileConfiguration config) {
        if (database == null || !config.getBoolean("logging.database.enabled", true)) {
            return;
        }
        logging.addSink(new DbLogSink(
                database.dataSource(), database.dialect(), database.tablePrefix("core"), getLogger()));
    }

    private void startDatabase(FileConfiguration config) {
        Dialect dialect = Dialect.fromType(config.getString("database.type", "sqlite"));
        File sqliteFile = new File(EcosystemData.root(this), config.getString("database.file", "database.db"));

        DatabaseSettings settings = new DatabaseSettings(
                dialect,
                sqliteFile,
                config.getString("database.host", "127.0.0.1"),
                config.getInt("database.port", 5432),
                config.getString("database.database", "antitle"),
                config.getString("database.username", "antitle"),
                config.getString("database.password", ""),
                readProperties(config.getConfigurationSection("database.properties")),
                config.getInt("database.pool-size", 10));

        // On a fresh install, create the database if it doesn't exist yet.
        if (config.getBoolean("database.create-if-missing", true)) {
            DatabaseBootstrap.ensureDatabaseExists(settings, log);
        }

        try {
            this.database = new HikariDatabaseService(settings);
        } catch (RuntimeException ex) {
            // Don't crash core (which would take its classloader down and cascade
            // to dependent plugins) — log clearly and leave DatabaseService
            // unregistered so DB-backed plugins disable themselves gracefully.
            log.error("Could not connect to the central database (type={}). "
                    + "DB-backed plugins (e.g. money) will not start.", dialect.name().toLowerCase());
            log.error("  Cause: {}", rootMessage(ex));
            log.error("  Check plugins/{}/config.yml [database] — host/port/credentials, "
                    + "and that the role & database exist.", EcosystemData.ROOT);
            return;
        }

        getServer().getServicesManager().register(
                DatabaseService.class, database, this, ServicePriority.Normal);

        if (dialect == Dialect.SQLITE) {
            log.info("Central database ready: sqlite at {}.", sqliteFile);
        } else {
            log.info("Central database ready: {} at {}:{}/{}.",
                    dialect.name().toLowerCase(), settings.host(), settings.port(), settings.database());
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null ? msg : cur.getClass().getSimpleName();
    }

    private static Map<String, String> readProperties(ConfigurationSection section) {
        Map<String, String> props = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                props.put(key, section.getString(key));
            }
        }
        return props;
    }
}
