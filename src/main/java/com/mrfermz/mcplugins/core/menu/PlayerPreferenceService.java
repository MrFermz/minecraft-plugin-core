package com.mrfermz.mcplugins.core.menu;

import java.util.Map;
import java.util.UUID;

/**
 * Stores and reads per-player option values, persisted in the shared central
 * database owned by core. Backed by an in-memory cache, so reads are cheap and
 * safe to call on the main thread (e.g. while rendering, or building
 * {@code /money top}); writes update the cache immediately — so a change takes
 * effect in real time — and persist asynchronously.
 *
 * <p>Consumers always pass a default so an option works before the player has
 * ever opened the menu. Look it up via
 * {@link com.mrfermz.mcplugins.core.CoreApi#preferences}.
 *
 * <pre>{@code
 * boolean onTop = prefs.getBoolean(uuid, "money.top.visible", true);
 * String style  = prefs.get(uuid, "healthbar.display", "bar");
 * }</pre>
 */
public interface PlayerPreferenceService {

    /** The raw stored value for {@code key}, or {@code def} if the player hasn't set it. */
    String get(UUID player, String key, String def);

    /** The value parsed as a boolean, or {@code def} if unset/unparseable. */
    boolean getBoolean(UUID player, String key, boolean def);

    /** The value parsed as an int, or {@code def} if unset/unparseable. */
    int getInt(UUID player, String key, int def);

    /** The value parsed as a double, or {@code def} if unset/unparseable. */
    double getDouble(UUID player, String key, double def);

    /**
     * Sets a value for the player. Updates the cache right away (so reads see it
     * immediately) and persists asynchronously.
     *
     * @param setBy who made the change (the player themselves, or {@code null} for
     *              console/system) — stored as {@code created_by}
     */
    void set(UUID player, String key, String value, UUID setBy);

    /** Snapshot of every stored value for the player (key → value). */
    Map<String, String> all(UUID player);
}
