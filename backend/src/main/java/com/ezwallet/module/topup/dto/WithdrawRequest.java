package com.ezwallet.module.topup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull Long bankAccountId,
        @NotNull @DecimalMin(value = "50000", message = "Số tiền rút tối thiểu là 50.000 ₫") BigDecimal amount,
        @NotBlank @Pattern(regexp = "\\d{6}") String otpCode,
        String idempotencyKey
) {}
