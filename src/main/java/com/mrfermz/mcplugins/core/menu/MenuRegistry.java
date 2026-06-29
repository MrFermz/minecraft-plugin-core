package com.mrfermz.mcplugins.core.menu;

import java.util.List;
import java.util.Optional;

/**
 * Registry of all per-player {@link MenuItem}s contributed by the plugins on the
 * server. Core owns the single registry instance (registered on Bukkit's
 * {@code ServicesManager}); feature plugins add their items on enable, and the
 * {@code Menu} plugin reads them to build the in-game UI.
 *
 * <pre>{@code
 * CoreApi.menu(getServer()).ifPresent(reg -> reg.register(
 *     MenuItem.toggle("money.top.visible", "Money",
 *         "Show me on /money top", "Appear on the public leaderboard", true)));
 * }</pre>
 */
public interface MenuRegistry {

    /**
     * Registers an item. Re-registering the same {@link MenuItem#key()} replaces
     * the previous one (so a plugin reload refreshes it rather than duplicating).
     */
    void register(MenuItem item);

    /** The item for a key, if one is registered. */
    Optional<MenuItem> get(String key);

    /** All registered items, in registration order. */
    List<MenuItem> all();
}
