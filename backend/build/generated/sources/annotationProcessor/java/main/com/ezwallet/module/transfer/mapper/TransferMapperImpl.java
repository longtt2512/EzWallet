package com.ezwallet.module.transfer.mapper;

import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transfer.dto.TransferResponse;
import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-09T23:48:30+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.7.jar, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class TransferMapperImpl implements TransferMapper {

    @Override
    public TransferResponse toDto(Transaction tx) {
        if ( tx == null ) {
            return null;
        }

        Long id = null;
        String transactionRef = null;
        TransactionType type = null;
        TransactionStatus status = null;
        BigDecimal amount = null;
        BigDecimal fee = null;
        String description = null;
        Instant completedAt = null;
        Instant createdAt = null;

        id = tx.getId();
        transactionRef = tx.getTransactionRef();
        type = tx.getType();
        status = tx.getStatus();
        amount = tx.getAmount();
        fee = tx.getFee();
        description = tx.getDescription();
        completedAt = tx.getCompletedAt();
        createdAt = tx.getCreatedAt();

        TransferResponse transferResponse = new TransferResponse( id, transactionRef, type, status, amount, fee, description, completedAt, createdAt );

        return transferResponse;
    }
}
