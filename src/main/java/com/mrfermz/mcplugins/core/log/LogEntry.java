package com.mrfermz.mcplugins.core.log;

/**
 * One immutable log line collected by the central {@link LogService}: what was
 * logged, by which plugin, at what severity and when.
 *
 * @param timestamp epoch millis when the line was emitted
 * @param level     severity
 * @param source    the emitting plugin's name (e.g. {@code "Money"}, {@code "Core"})
 * @param message   the formatted message
 * @param error     a stack trace for error lines, or {@code null}
 */
public record LogEntry(long timestamp, LogLevel level, String source, String message, String error) {
}
