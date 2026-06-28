package com.mrfermz.mcplugins.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Resolves the single, shared on-server config/data directory for the whole
 * ecosystem.
 *
 * <p>Bukkit hands each plugin its own {@code plugins/<PluginName>/} folder
 * (and {@link Plugin#getDataFolder()} is {@code final}, so it cannot be
 * overridden). To keep every plugin's files together instead of scattered, all
 * modules resolve their files through here under {@code plugins/}{@value #ROOT}{@code /}.
 *
 * <p>Each feature plugin gets a single flat config file named after the module —
 * {@code plugins/}{@value #ROOT}{@code /<module>.yml} — so there are no nested
 * per-plugin subfolders. The shared/global settings owned by core live in the
 * root {@code config.yml}.
 *
 * <pre>{@code
 * // plugins/antitle/money.yml (default seeded from the jar's config.yml on first run)
 * FileConfiguration cfg = EcosystemData.config(this, "money");
 * // plugins/antitle/config.yml (global, owned by core)
 * FileConfiguration global = EcosystemData.config(this);
 * }</pre>
 *
 * <p>Do NOT use {@code getDataFolder()}, {@code getConfig()} or
 * {@code saveDefaultConfig()} directly in feature plugins — those are pinned to
 * the per-plugin folder and would split files back out (see CLAUDE.md →
 * "Config directory บน server").
 */
public final class EcosystemData {

    /** Name of the single shared directory under {@code plugins/}. */
    public static final String ROOT = "antitle";

    private EcosystemData() {
    }

    /** The shared root, {@code plugins/}{@value #ROOT}{@code /}. */
    public static File root(Plugin plugin) {
        // getDataFolder() is plugins/<PluginName>/ (a path; need not exist yet);
        // its parent is the server's plugins/ directory.
        return new File(plugin.getDataFolder().getParentFile(), ROOT);
    }

    /**
     * Returns {@code plugins/}{@value #ROOT}{@code /<module>/}, creating it if
     * necessary. Only needed by plugins that store loose data files in their own
     * directory; plain config uses the flat {@link #config(Plugin, String)} file.
     */
    public static File folder(Plugin plugin, String module) {
        File dir = new File(root(plugin), module);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder " + dir);
        }
        return dir;
    }

    /**
     * Loads {@code <module>.yml} from the shared root, seeding it from the
     * {@code config.yml} bundled in the calling plugin's jar on first run.
     * Replaces {@code saveDefaultConfig()} + {@code getConfig()} for feature
     * plugins. Use the module's short name (e.g. {@code "money"}).
     */
    public static FileConfiguration config(Plugin plugin, String module) {
        File dir = ensureRoot(plugin);
        return loadConfig(plugin, new File(dir, module + ".yml"));
    }

    /**
     * Loads the shared root {@code config.yml} ({@code plugins/}{@value #ROOT}{@code
     * /config.yml}) — global settings owned by core (DB pool, web-config client).
     */
    public static FileConfiguration config(Plugin plugin) {
        File dir = ensureRoot(plugin);
        return loadConfig(plugin, new File(dir, "config.yml"));
    }

    private static File ensureRoot(Plugin plugin) {
        File dir = root(plugin);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder " + dir);
        }
        return dir;
    }

    private static FileConfiguration loadConfig(Plugin plugin, File file) {
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Could not write default config to " + file, ex);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
