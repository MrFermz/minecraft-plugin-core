package com.mrfermz.mcplugins.core.log;

import com.mrfermz.mcplugins.core.db.Dialect;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * {@link LogSink} that writes log lines to the central database table
 * {@code core_logs}, using the shared pool owned by core (never its own).
 *
 * <p>SQL is {@link Dialect}-aware so the same code runs on SQLite, PostgreSQL and
 * MySQL/MariaDB. Inserts are batched in one transaction per flush. The DB pool is
 * owned and closed by core, so {@link #close()} here is a no-op.
 */
public final class DbLogSink implements LogSink {

    private static final String COLUMNS = "ts, level, source, message, error";

    private final DataSource dataSource;
    private final Dialect dialect;
    private final Logger errorLog;
    private final String table;

    public DbLogSink(DataSource dataSource, Dialect dialect, String tablePrefix, Logger errorLog) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.errorLog = errorLog;
        // tablePrefix comes from core (e.g. "core_"), not user input.
        this.table = tablePrefix + "logs";
        createTable();
    }

    private void createTable() {
        // Generated UUID surrogate id as PK (ecosystem convention) — VARCHAR(36)
        // works on every engine; no per-dialect auto-increment.
        String ddl = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "id VARCHAR(36) PRIMARY KEY, "
                + "ts BIGINT NOT NULL, "
                + "level VARCHAR(8) NOT NULL, "
                + "source VARCHAR(64) NOT NULL, "
                + "message TEXT NOT NULL, "
                + "error TEXT)";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ddl);
        } catch (SQLException ex) {
            errorLog.log(Level.SEVERE, "Failed to create table " + table, ex);
            return;
        }
        safeIndex("idx_" + table + "_ts", "ts");
        safeIndex("idx_" + table + "_source", "source");
        safeIndex("idx_" + table + "_level", "level");
    }

    /** Creates an index, tolerating "already exists" (MySQL lacks IF NOT EXISTS). */
    private void safeIndex(String name, String column) {
        String ifNotExists = dialect.isMySqlFamily() ? "" : "IF NOT EXISTS ";
        String sql = "CREATE INDEX " + ifNotExists + name + " ON " + table + " (" + column + ")";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException ex) {
            if (!dialect.isMySqlFamily()) {
                errorLog.warning("Could not create index " + name + " on " + table + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public void write(List<LogEntry> batch) {
        // During shutdown the pool may already be closed (core closes the DB
        // before the final flush); skip quietly rather than spamming errors —
        // these lines still reach the console and file sink.
        if (dataSource instanceof HikariDataSource pool && pool.isClosed()) {
            return;
        }
        String sql = "INSERT INTO " + table + " (id, " + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (LogEntry e : batch) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setLong(2, e.timestamp());
                    ps.setString(3, e.level().name());
                    ps.setString(4, e.source());
                    ps.setString(5, e.message());
                    ps.setString(6, e.error());
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
        } catch (SQLException ex) {
            errorLog.log(Level.SEVERE, "Failed to write " + batch.size() + " log entries to " + table, ex);
        }
    }

    @Override
    public void close() {
        // Pool is owned by core.
    }
}
