package com.ezwallet.module.bill.dto;

import com.ezwallet.module.bill.entity.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillResponse(
        Long id,
        String billCode,
        String providerName,
        String providerCode,
        String customerCode,
        BigDecimal amount,
        BillStatus status,
        LocalDate dueDate
) {}
