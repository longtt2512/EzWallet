package com.ezwallet.module.transaction.dto;

import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;

import java.time.Instant;

public record TransactionFilterRequest(
        TransactionType type,
        TransactionStatus status,
        Instant dateFrom,
        Instant dateTo,
        int page,
        int size
) {
    public TransactionFilterRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 10;
    }
}
