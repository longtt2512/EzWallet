package com.ezwallet.module.topup.dto;

import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String transactionRef,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String description,
        Instant completedAt,
        Instant createdAt,
        String direction,  // "CREDIT" or "DEBIT" — null when not relevant
        BigDecimal sourceBalanceBefore,
        BigDecimal sourceBalanceAfter,
        BigDecimal targetBalanceBefore,
        BigDecimal targetBalanceAfter
) {}
