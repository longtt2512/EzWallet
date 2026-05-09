package com.ezwallet.module.topup.mapper;

import com.ezwallet.module.account.entity.BankAccount;
import com.ezwallet.module.topup.dto.BankAccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {

    @Mapping(target = "maskedAccountNumber", expression = "java(mask(b.getAccountNumber()))")
    @Mapping(target = "isDefault", source = "default")
    BankAccountResponse toDto(BankAccount b);

    default String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return accountNumber;
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }
}
