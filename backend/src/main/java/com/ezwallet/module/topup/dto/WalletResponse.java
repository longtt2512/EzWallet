package com.ezwallet.module.topup.dto;

import com.ezwallet.module.account.entity.WalletStatus;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        String currency,
        WalletStatus status
) {}
