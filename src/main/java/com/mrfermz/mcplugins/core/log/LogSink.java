package com.mrfermz.mcplugins.core.log;

import java.util.List;

/**
 * A backend the {@link LogService} fans log lines out to (a file, the central
 * DB, …). The service owns all buffering and threading: it calls {@link #write}
 * with a batch off the main thread, one batch at a time, so implementations can
 * keep their I/O straightforward and need not be concurrent.
 */
public interface LogSink {

    /** Persists a batch of log lines. Implementations must not block the main thread. */
    void write(List<LogEntry> batch);

    /** Flushes and releases any resources (files, etc.). The DB pool is not owned here. */
    void close();
}
