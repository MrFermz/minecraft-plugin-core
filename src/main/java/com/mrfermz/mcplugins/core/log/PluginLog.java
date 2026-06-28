package com.mrfermz.mcplugins.core.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Central logging wrapper so every plugin in the ecosystem formats messages the
 * same way (see CLAUDE.md → Conventions). Wraps the plugin's own Bukkit
 * {@link Logger} and supports SLF4J-style {@code {}} placeholders.
 *
 * <p>Besides printing to the server console, every line is forwarded to the
 * centralized {@link LogService} (file + central DB) registered by
 * {@code minecraft-plugin-core}, so all operational logs are persisted in one
 * place with no extra wiring per plugin. The service is looked up lazily and
 * cached: lines emitted before core registers it (very early startup) print to
 * console only, and from then on everything is persisted too.
 *
 * <pre>{@code
 * PluginLog log = PluginLog.of(this);
 * log.info("Loaded {} accounts in {} ms", count, elapsed);
 * }</pre>
 */
public final class PluginLog {

    private final Plugin plugin;
    private final Logger logger;
    private final String source;
    private LogService service; // resolved lazily, then cached

    private PluginLog(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.source = plugin.getName();
    }

    public static PluginLog of(Plugin plugin) {
        return new PluginLog(plugin);
    }

    public void info(String message, Object... args) {
        emit(LogLevel.INFO, format(message, args), null);
    }

    public void warn(String message, Object... args) {
        emit(LogLevel.WARN, format(message, args), null);
    }

    public void error(String message, Object... args) {
        emit(LogLevel.ERROR, format(message, args), null);
    }

    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
        forward(LogLevel.ERROR, message, stackTrace(t));
    }

    private void emit(LogLevel level, String message, String error) {
        logger.log(level.toJul(), message);
        forward(level, message, error);
    }

    /** Sends the line to the central log service if it's available yet. */
    private void forward(LogLevel level, String message, String error) {
        LogService svc = service();
        if (svc != null) {
            svc.log(new LogEntry(System.currentTimeMillis(), level, source, message, error));
        }
    }

    private LogService service() {
        if (service == null) {
            RegisteredServiceProvider<LogService> rsp =
                    plugin.getServer().getServicesManager().getRegistration(LogService.class);
            if (rsp != null) {
                service = rsp.getProvider();
            }
        }
        return service;
    }

    private static String stackTrace(Throwable t) {
        if (t == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String format(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 16 * args.length);
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            if (argIndex < args.length && i + 1 < template.length()
                    && template.charAt(i) == '{' && template.charAt(i + 1) == '}') {
                sb.append(String.valueOf(args[argIndex++]));
                i += 2;
            } else {
                sb.append(template.charAt(i++));
            }
        }
        return sb.toString();
    }
}
