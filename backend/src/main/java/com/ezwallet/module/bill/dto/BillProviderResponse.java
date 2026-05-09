package com.ezwallet.module.bill.dto;

public record BillProviderResponse(
        Long id,
        String code,
        String name,
        String serviceType
) {}
