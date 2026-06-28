package com.mrfermz.mcplugins.core.log;

import com.mrfermz.mcplugins.core.db.Dialect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
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
        String idCol = switch (dialect) {
            case SQLITE -> "id INTEGER PRIMARY KEY AUTOINCREMENT";
            case POSTGRESQL -> "id BIGSERIAL PRIMARY KEY";
            case MYSQL, MARIADB -> "id BIGINT AUTO_INCREMENT PRIMARY KEY";
        };
        String ddl = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + idCol + ", "
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
        String sql = "INSERT INTO " + table + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (LogEntry e : batch) {
                    ps.setLong(1, e.timestamp());
                    ps.setString(2, e.level().name());
                    ps.setString(3, e.source());
                    ps.setString(4, e.message());
                    ps.setString(5, e.error());
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
