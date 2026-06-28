package com.mrfermz.mcplugins.core.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The shared economy contract for the whole ecosystem.
 *
 * <p>The {@code minecraft-plugin-money} plugin provides the implementation and
 * registers it through Bukkit's {@code ServicesManager}. Any other plugin (the
 * "main system" that comes later) consumes currency purely through this
 * interface — never by referencing money's internal classes. Look it up with
 * {@link com.mrfermz.mcplugins.core.CoreApi#economy(org.bukkit.Server)}.
 *
 * <p>All amounts are {@link BigDecimal} so currency math stays exact; never use
 * {@code double} for balances. Implementations must reject negative amounts on
 * deposit/withdraw and treat balances as non-negative unless overdraft is
 * explicitly enabled by the implementation.
 *
 * <p>Methods are safe to call from the main server thread; implementations are
 * expected to keep an in-memory cache and persist asynchronously.
 */
public interface EconomyService {

    /** @return the configured currency name, e.g. {@code "Coin"}. */
    String currencyNameSingular();

    /** @return the plural currency name, e.g. {@code "Coins"}. */
    String currencyNamePlural();

    /** Formats an amount for display, e.g. {@code "$1,250.00"}. */
    String format(BigDecimal amount);

    /** @return {@code true} if the account exists / has been seen before. */
    boolean hasAccount(UUID player);

    /** @return the current balance, or the starting balance if not yet seen. */
    BigDecimal getBalance(UUID player);

    /** @return {@code true} if the player can afford {@code amount}. */
    boolean has(UUID player, BigDecimal amount);

    /** Adds {@code amount} to the player's balance. */
    EconomyResponse deposit(UUID player, BigDecimal amount);

    /** Removes {@code amount} from the player's balance if affordable. */
    EconomyResponse withdraw(UUID player, BigDecimal amount);

    /** Sets the balance to an exact value (admin operation). */
    EconomyResponse setBalance(UUID player, BigDecimal amount);

    /** Moves {@code amount} from one player to another atomically. */
    EconomyResponse transfer(UUID from, UUID to, BigDecimal amount);
}
