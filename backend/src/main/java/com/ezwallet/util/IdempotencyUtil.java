package com.ezwallet.util;

import com.ezwallet.module.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class IdempotencyUtil {

    private IdempotencyUtil() {}

    /** Unique transaction reference: TXP-<UUID without dashes, 16 chars upper> */
    public static String generateTxRef() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "TXP-" + uuid.substring(0, 16);
    }

    /** Deterministic idempotency key scoped to (userId, type, amount, epoch-minute) */
    public static String generateIdempotencyKey(Long userId, TransactionType type, BigDecimal amount) {
        long epochMinute = Instant.now().getEpochSecond() / 60;
        return String.format("%s:%s:%s:%d", userId, type.name(), amount.toPlainString(), epochMinute);
    }
}
