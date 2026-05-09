package com.ezwallet.module.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record P2PTransferRequest(
        @NotBlank String receiverIdentifier,
        @NotNull @DecimalMin(value = "10000", message = "Số tiền chuyển tối thiểu là 10.000 ₫") BigDecimal amount,
        String note,
        @NotBlank @Pattern(regexp = "\\d{6}") String otpCode,
        String idempotencyKey
) {}
