package com.ezwallet.module.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record QrResponse(
        String qrId,
        String qrImageUrl,
        BigDecimal amount,
        String note,
        Instant expiresAt
) {}
