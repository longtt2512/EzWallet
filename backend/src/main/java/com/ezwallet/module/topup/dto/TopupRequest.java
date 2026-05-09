package com.ezwallet.module.topup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopupRequest(
        @NotNull Long bankAccountId,
        @NotNull @DecimalMin(value = "10000", message = "Số tiền nạp tối thiểu là 10.000 ₫") BigDecimal amount,
        @NotBlank String otpCode,
        String idempotencyKey
) {}
