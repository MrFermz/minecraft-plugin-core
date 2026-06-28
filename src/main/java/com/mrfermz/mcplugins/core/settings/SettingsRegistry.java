package com.mrfermz.mcplugins.core.settings;

import java.util.List;
import java.util.Optional;

/**
 * Registry of all per-player {@link SettingDefinition}s contributed by the
 * plugins on the server. Core owns the single registry instance (registered on
 * Bukkit's {@code ServicesManager}); feature plugins add their settings on
 * enable, and the {@code Settings} plugin reads them to build the in-game UI.
 *
 * <pre>{@code
 * CoreApi.settings(getServer()).ifPresent(reg -> reg.register(
 *     SettingDefinition.toggle("money.top.visible", "Money",
 *         "Show me on /money top", "Appear on the public leaderboard", true)));
 * }</pre>
 */
public interface SettingsRegistry {

    /**
     * Registers a setting. Re-registering the same {@link SettingDefinition#key()}
     * replaces the previous definition (so a plugin reload refreshes it rather
     * than duplicating).
     */
    void register(SettingDefinition definition);

    /** The definition for a key, if one is registered. */
    Optional<SettingDefinition> get(String key);

    /** All registered definitions, in registration order. */
    List<SettingDefinition> all();
}
