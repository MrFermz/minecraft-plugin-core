package com.mrfermz.mcplugins.core.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Default {@link LogService}: buffers entries in a queue and fans them out to its
 * {@link LogSink}s off the main thread in batches.
 *
 * <p>{@link #log} drops anything below the configured minimum level, enqueues the
 * rest and kicks a debounced async flush (only one flush is in flight at a time,
 * and it drains the whole queue). {@code CorePlugin} also runs a periodic flush
 * as a safety net and a final flush on disable. Sinks are called one batch at a
 * time, so a sink's {@link LogSink#write} need not be thread-safe.
 *
 * <p>Sink failures are reported straight to the core plugin's
 * {@code java.util.logging} logger — never back through {@link PluginLog} — so a
 * broken sink can't recurse into the log pipeline.
 */
public final class DefaultLogService implements LogService {

    private final Plugin plugin;
    private final Logger errorLog;
    private final LogLevel minLevel;
    private final List<LogSink> sinks = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<LogEntry> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean flushQueued = new AtomicBoolean(false);

    public DefaultLogService(Plugin plugin, LogLevel minLevel) {
        this.plugin = plugin;
        this.errorLog = plugin.getLogger();
        this.minLevel = minLevel;
    }

    /** Adds a backend. Safe to call after the service is already registered. */
    public void addSink(LogSink sink) {
        sinks.add(sink);
    }

    /** @return true if at least one sink is attached. */
    public boolean hasSinks() {
        return !sinks.isEmpty();
    }

    @Override
    public void log(LogEntry entry) {
        if (!entry.level().atLeast(minLevel) || sinks.isEmpty()) {
            return;
        }
        queue.add(entry);
        // Only schedule async work while enabled; Folia rejects task registration
        // once the plugin is disabling. Lines logged during shutdown stay queued
        // and are drained by the explicit flush()/close() in CorePlugin.onDisable.
        if (plugin.isEnabled() && flushQueued.compareAndSet(false, true)) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
                flushQueued.set(false);
                flush();
            });
        }
    }

    @Override
    public synchronized void flush() {
        if (queue.isEmpty()) {
            return;
        }
        List<LogEntry> batch = new ArrayList<>();
        for (LogEntry e; (e = queue.poll()) != null; ) {
            batch.add(e);
        }
        for (LogSink sink : sinks) {
            try {
                sink.write(batch);
            } catch (RuntimeException ex) {
                errorLog.log(Level.SEVERE,
                        "Log sink " + sink.getClass().getSimpleName() + " failed to write "
                                + batch.size() + " entries", ex);
            }
        }
    }

    /** Flushes remaining entries and closes every sink. */
    public void close() {
        flush();
        for (LogSink sink : sinks) {
            try {
                sink.close();
            } catch (RuntimeException ex) {
                errorLog.log(Level.SEVERE,
                        "Log sink " + sink.getClass().getSimpleName() + " failed to close", ex);
            }
        }
    }
}
