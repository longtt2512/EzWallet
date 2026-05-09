package com.ezwallet.module.topup.dto;

public record BankAccountResponse(
        Long id,
        String bankCode,
        String maskedAccountNumber,
        String accountHolder,
        boolean isDefault,
        boolean verified
) {}
