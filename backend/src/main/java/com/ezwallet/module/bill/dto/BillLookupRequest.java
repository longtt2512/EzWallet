package com.ezwallet.module.bill.dto;

import jakarta.validation.constraints.NotBlank;

public record BillLookupRequest(
        @NotBlank String providerCode,
        @NotBlank String customerCode
) {}
