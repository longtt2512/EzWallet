package com.ezwallet.module.topup.mapper;

import com.ezwallet.module.account.entity.BankAccount;
import com.ezwallet.module.topup.dto.BankAccountResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-09T16:26:35+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.7.jar, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class BankAccountMapperImpl implements BankAccountMapper {

    @Override
    public BankAccountResponse toDto(BankAccount b) {
        if ( b == null ) {
            return null;
        }

        boolean isDefault = false;
        Long id = null;
        String bankCode = null;
        String accountHolder = null;
        boolean verified = false;

        isDefault = b.isDefault();
        id = b.getId();
        bankCode = mask( b.getBankCode() );
        accountHolder = mask( b.getAccountHolder() );
        verified = b.isVerified();

        String maskedAccountNumber = mask(b.getAccountNumber());

        BankAccountResponse bankAccountResponse = new BankAccountResponse( id, bankCode, maskedAccountNumber, accountHolder, isDefault, verified );

        return bankAccountResponse;
    }
}
