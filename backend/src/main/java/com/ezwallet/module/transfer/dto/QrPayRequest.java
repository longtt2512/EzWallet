package com.ezwallet.module.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QrPayRequest(
        @NotBlank String qrId,
        @NotBlank @Pattern(regexp = "\\d{6}") String otpCode,
        String idempotencyKey
) {}
