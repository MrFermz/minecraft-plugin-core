package com.mrfermz.mcplugins.core.api;

import java.math.BigDecimal;

/**
 * Outcome of an {@link EconomyService} mutation.
 *
 * @param success    whether the operation was applied
 * @param amount     the amount that was requested
 * @param newBalance the resulting balance of the primary account
 * @param error      the failure reason, or {@link Error#NONE} on success
 */
public record EconomyResponse(boolean success, BigDecimal amount, BigDecimal newBalance, Error error) {

    public enum Error {
        NONE,
        INSUFFICIENT_FUNDS,
        NEGATIVE_AMOUNT,
        ACCOUNT_NOT_FOUND,
        STORAGE_ERROR
    }

    public static EconomyResponse ok(BigDecimal amount, BigDecimal newBalance) {
        return new EconomyResponse(true, amount, newBalance, Error.NONE);
    }

    public static EconomyResponse fail(Error error, BigDecimal amount, BigDecimal currentBalance) {
        return new EconomyResponse(false, amount, currentBalance, error);
    }
}
