package com.ezwallet.module.topup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankAccountRequest(
        @NotBlank @Size(max = 20) String bankCode,
        @NotBlank @Size(max = 30) String accountNumber,
        @NotBlank @Size(max = 150) String accountHolder
) {}
