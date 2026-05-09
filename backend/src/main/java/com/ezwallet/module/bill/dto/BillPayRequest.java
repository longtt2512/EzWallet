package com.ezwallet.module.bill.dto;

import jakarta.validation.constraints.NotBlank;

public record BillPayRequest(
        @NotBlank String billCode,
        String otpCode,
        String idempotencyKey
) {}
