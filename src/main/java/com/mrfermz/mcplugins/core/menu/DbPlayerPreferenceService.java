package com.mrfermz.mcplugins.core.menu;

import com.mrfermz.mcplugins.core.db.Dialect;
import com.mrfermz.mcplugins.core.log.PluginLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.bukkit.plugin.Plugin;

/**
 * Database-backed {@link PlayerPreferenceService}. Stores every player's option
 * values in the shared central DB via core's {@link DataSource} — no extra pool
 * (CLAUDE.md → Database). The table is namespaced {@code setting_values}.
 *
 * <p>All values live in an in-memory cache loaded once at startup (the data is
 * low-volume), so reads are instant for online and offline players alike — which
 * is what lets {@code /money top} filter by an offline player's choice. Writes
 * update the cache immediately and are flushed to the DB off the main thread in a
 * debounced batch, mirroring how money persists balances.
 *
 * <p>SQL is {@link Dialect}-aware (SQLite / PostgreSQL / MySQL / MariaDB).
 */
public final class DbPlayerPreferenceService implements PlayerPreferenceService {

    /** One pending write, keyed in {@link #pending} by player + key. */
    private record Pending(UUID player, String key, String value, UUID setBy, Instant at) {
    }

    private final Plugin plugin;
    private final DataSource dataSource;
    private final Dialect dialect;
    private final PluginLog log;
    private final String table;

    // player -> (key -> value). Loaded at startup; the source of truth for reads.
    private final Map<UUID, Map<String, String>> cache = new ConcurrentHashMap<>();
    // Buffered writes, keyed by "player key" so repeated edits coalesce.
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean flushQueued = new AtomicBoolean(false);

    public DbPlayerPreferenceService(Plugin plugin, DataSource dataSource, String tablePrefix,
                                     Dialect dialect, PluginLog log) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.log = log;
        // tablePrefix comes from core (e.g. "setting_"), not user input.
        this.table = tablePrefix + "values";
        createTable();
        loadAll();
    }

    private void createTable() {
        // Generated UUID surrogate id as PK (ecosystem convention); (player_uuid,
        // setting_key) is the UNIQUE natural key used for upserts. created_at is a
        // real date column (DATETIME on MySQL, TIMESTAMP elsewhere); created_by is
        // who last changed it (null = console/system).
        String tsType = dialect.isMySqlFamily() ? "DATETIME" : "TIMESTAMP";
        String valueType = dialect.isMySqlFamily() ? "VARCHAR(512)" : "TEXT";
        String ddl = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "id VARCHAR(36) PRIMARY KEY, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "setting_key VARCHAR(128) NOT NULL, "
                + "setting_value " + valueType + " NOT NULL, "
                + "created_at " + tsType + " NOT NULL, "
                + "created_by VARCHAR(36), "
                + "CONSTRAINT uq_" + table + " UNIQUE (player_uuid, setting_key))";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ddl);
        } catch (SQLException ex) {
            log.error("Failed to create table " + table, ex);
        }
    }

    private void loadAll() {
        String sql = "SELECT player_uuid, setting_key, setting_value FROM " + table;
        int rows = 0;
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    UUID player = UUID.fromString(rs.getString("player_uuid"));
                    cache.computeIfAbsent(player, p -> new ConcurrentHashMap<>())
                            .put(rs.getString("setting_key"), rs.getString("setting_value"));
                    rows++;
                } catch (IllegalArgumentException badRow) {
                    log.warn("Skipping malformed row in {}: player_uuid={}",
                            table, rs.getString("player_uuid"));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to load player preferences from " + table, ex);
        }
        log.info("Loaded {} player preference(s) from {}.", rows, table);
    }

    // ----- reads -----

    @Override
    public String get(UUID player, String key, String def) {
        Map<String, String> values = cache.get(player);
        if (values == null) {
            return def;
        }
        String value = values.get(key);
        return value != null ? value : def;
    }

    @Override
    public boolean getBoolean(UUID player, String key, boolean def) {
        String value = get(player, key, null);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    @Override
    public int getInt(UUID player, String key, int def) {
        String value = get(player, key, null);
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    @Override
    public double getDouble(UUID player, String key, double def) {
        String value = get(player, key, null);
        if (value == null) {
            return def;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    @Override
    public Map<String, String> all(UUID player) {
        Map<String, String> values = cache.get(player);
        return values == null ? Map.of() : new HashMap<>(values);
    }

    // ----- writes -----

    @Override
    public void set(UUID player, String key, String value, UUID setBy) {
        // Update the cache first so reads see the new value immediately (realtime).
        cache.computeIfAbsent(player, p -> new ConcurrentHashMap<>()).put(key, value);
        pending.put(player + " " + key, new Pending(player, key, value, setBy, Instant.now()));
        if (flushQueued.compareAndSet(false, true)) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
                flushQueued.set(false);
                flush();
            });
        }
    }

    /** Persists buffered writes; safe to call off the main thread. */
    public synchronized void flush() {
        if (pending.isEmpty()) {
            return;
        }
        Map<String, Pending> batch = new HashMap<>(pending);
        String sql = upsertSql();
        try (Connection conn = dataSource.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Pending p : batch.values()) {
                    // New row id is only used on insert; on conflict the existing
                    // row's id is kept.
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, p.player().toString());
                    ps.setString(3, p.key());
                    ps.setString(4, p.value());
                    ps.setTimestamp(5, Timestamp.from(p.at()));
                    ps.setString(6, p.setBy() == null ? null : p.setBy().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
            batch.keySet().forEach(pending::remove);
        } catch (SQLException ex) {
            log.error("Failed to flush " + batch.size() + " player preference(s) to " + table, ex);
        }
    }

    /** Flushes any pending writes (called on disable). */
    public void close() {
        flush();
    }

    private String upsertSql() {
        String columns = "(id, player_uuid, setting_key, setting_value, created_at, created_by)";
        String values = "VALUES (?, ?, ?, ?, ?, ?)";
        if (dialect.isMySqlFamily()) {
            return "INSERT INTO " + table + " " + columns + " " + values
                    + " ON DUPLICATE KEY UPDATE "
                    + "setting_value = VALUES(setting_value), "
                    + "created_at = VALUES(created_at), "
                    + "created_by = VALUES(created_by)";
        }
        return "INSERT INTO " + table + " " + columns + " " + values
                + " ON CONFLICT (player_uuid, setting_key) DO UPDATE SET "
                + "setting_value = excluded.setting_value, "
                + "created_at = excluded.created_at, "
                + "created_by = excluded.created_by";
    }
}
