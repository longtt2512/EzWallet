package com.ezwallet.module.transfer.dto;

import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        String transactionRef,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal fee,
        String description,
        Instant completedAt,
        Instant createdAt
) {}
