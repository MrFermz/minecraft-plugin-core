package com.mrfermz.mcplugins.core.log;

import java.util.Locale;
import java.util.logging.Level;

/**
 * Severity of a {@link LogEntry}, decoupled from {@code java.util.logging} so the
 * central log model is the same on every backend (console, file, DB).
 */
public enum LogLevel {

    INFO,
    WARN,
    ERROR;

    /** @return true if this level is at least as severe as {@code min}. */
    public boolean atLeast(LogLevel min) {
        return ordinal() >= min.ordinal();
    }

    /** Maps to the matching {@code java.util.logging} level for console output. */
    public Level toJul() {
        return switch (this) {
            case INFO -> Level.INFO;
            case WARN -> Level.WARNING;
            case ERROR -> Level.SEVERE;
        };
    }

    /** Parses the {@code logging.level} config value; unknown values → {@link #INFO}. */
    public static LogLevel fromConfig(String value) {
        if (value == null) {
            return INFO;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "warn", "warning" -> WARN;
            case "error", "severe" -> ERROR;
            default -> INFO;
        };
    }
}
