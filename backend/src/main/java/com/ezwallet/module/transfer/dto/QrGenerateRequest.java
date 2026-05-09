package com.ezwallet.module.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record QrGenerateRequest(
        @NotNull @DecimalMin(value = "10000", message = "Số tiền tối thiểu là 10.000 ₫") BigDecimal amount,
        String note,
        Integer expiresInMinutes
) {}
