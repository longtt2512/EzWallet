package com.ezwallet.module.topup.mapper;

import com.ezwallet.module.topup.dto.TransactionResponse;
import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-09T22:08:06+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.7.jar, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public TransactionResponse toDto(Transaction tx) {
        if ( tx == null ) {
            return null;
        }

        Long id = null;
        String transactionRef = null;
        TransactionType type = null;
        TransactionStatus status = null;
        BigDecimal amount = null;
        BigDecimal fee = null;
        String currency = null;
        String description = null;
        Instant completedAt = null;
        Instant createdAt = null;
        BigDecimal sourceBalanceBefore = null;
        BigDecimal sourceBalanceAfter = null;
        BigDecimal targetBalanceBefore = null;
        BigDecimal targetBalanceAfter = null;

        id = tx.getId();
        transactionRef = tx.getTransactionRef();
        type = tx.getType();
        status = tx.getStatus();
        amount = tx.getAmount();
        fee = tx.getFee();
        currency = tx.getCurrency();
        description = tx.getDescription();
        completedAt = tx.getCompletedAt();
        createdAt = tx.getCreatedAt();
        sourceBalanceBefore = tx.getSourceBalanceBefore();
        sourceBalanceAfter = tx.getSourceBalanceAfter();
        targetBalanceBefore = tx.getTargetBalanceBefore();
        targetBalanceAfter = tx.getTargetBalanceAfter();

        String direction = null;

        TransactionResponse transactionResponse = new TransactionResponse( id, transactionRef, type, status, amount, fee, currency, description, completedAt, createdAt, direction, sourceBalanceBefore, sourceBalanceAfter, targetBalanceBefore, targetBalanceAfter );

        return transactionResponse;
    }
}
