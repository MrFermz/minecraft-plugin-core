package com.mrfermz.mcplugins.core;

import com.mrfermz.mcplugins.core.api.EconomyService;
import com.mrfermz.mcplugins.core.db.DatabaseService;
import com.mrfermz.mcplugins.core.menu.MenuRegistry;
import com.mrfermz.mcplugins.core.menu.PlayerPreferenceService;
import java.util.Optional;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Convenience accessors for the shared services registered on Bukkit's
 * {@code ServicesManager}. This is the front door other plugins use instead of
 * reaching into another plugin's internals.
 *
 * <pre>{@code
 * EconomyService eco = CoreApi.economy(getServer())
 *     .orElseThrow(() -> new IllegalStateException("money plugin not loaded"));
 * }</pre>
 */
public final class CoreApi {

    private CoreApi() {
    }

    /** Looks up the registered {@link EconomyService}, if any plugin provides one. */
    public static Optional<EconomyService> economy(Server server) {
        RegisteredServiceProvider<EconomyService> rsp =
                server.getServicesManager().getRegistration(EconomyService.class);
        return rsp == null ? Optional.empty() : Optional.of(rsp.getProvider());
    }

    /** Looks up the shared {@link DatabaseService} (the one central DB pool). */
    public static Optional<DatabaseService> database(Server server) {
        RegisteredServiceProvider<DatabaseService> rsp =
                server.getServicesManager().getRegistration(DatabaseService.class);
        return rsp == null ? Optional.empty() : Optional.of(rsp.getProvider());
    }

    /**
     * Looks up the shared {@link MenuRegistry} — where feature plugins register
     * their per-player options for the in-game menu.
     */
    public static Optional<MenuRegistry> menu(Server server) {
        RegisteredServiceProvider<MenuRegistry> rsp =
                server.getServicesManager().getRegistration(MenuRegistry.class);
        return rsp == null ? Optional.empty() : Optional.of(rsp.getProvider());
    }

    /** Looks up the shared {@link PlayerPreferenceService} (per-player setting values). */
    public static Optional<PlayerPreferenceService> preferences(Server server) {
        RegisteredServiceProvider<PlayerPreferenceService> rsp =
                server.getServicesManager().getRegistration(PlayerPreferenceService.class);
        return rsp == null ? Optional.empty() : Optional.of(rsp.getProvider());
    }
}
