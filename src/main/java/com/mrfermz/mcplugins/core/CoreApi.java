package com.mrfermz.mcplugins.core;

import com.mrfermz.mcplugins.core.api.EconomyService;
import com.mrfermz.mcplugins.core.db.DatabaseService;
import com.mrfermz.mcplugins.core.log.LogService;
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
     * Looks up the centralized {@link LogService}. Most plugins don't need this
     * directly — logging through {@code PluginLog} already forwards here.
     */
    public static Optional<LogService> logging(Server server) {
        RegisteredServiceProvider<LogService> rsp =
                server.getServicesManager().getRegistration(LogService.class);
        return rsp == null ? Optional.empty() : Optional.of(rsp.getProvider());
    }
}
