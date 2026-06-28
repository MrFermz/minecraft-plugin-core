package com.mrfermz.mcplugins.core.log;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Central logging wrapper so every plugin in the ecosystem formats messages the
 * same way (see CLAUDE.md → Conventions). Wraps the plugin's own Bukkit
 * {@link Logger} and supports SLF4J-style {@code {}} placeholders.
 *
 * <pre>{@code
 * PluginLog log = PluginLog.of(this);
 * log.info("Loaded {} accounts in {} ms", count, elapsed);
 * }</pre>
 */
public final class PluginLog {

    private final Logger logger;

    private PluginLog(Logger logger) {
        this.logger = logger;
    }

    public static PluginLog of(Plugin plugin) {
        return new PluginLog(plugin.getLogger());
    }

    public void info(String message, Object... args) {
        logger.log(Level.INFO, format(message, args));
    }

    public void warn(String message, Object... args) {
        logger.log(Level.WARNING, format(message, args));
    }

    public void error(String message, Object... args) {
        logger.log(Level.SEVERE, format(message, args));
    }

    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
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
