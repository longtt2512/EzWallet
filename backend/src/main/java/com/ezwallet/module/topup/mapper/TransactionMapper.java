package com.ezwallet.module.topup.mapper;

import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.topup.dto.TransactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @org.mapstruct.Mapping(target = "direction", ignore = true)
    TransactionResponse toDto(Transaction tx);

    default TransactionResponse toDto(Transaction tx, Long walletId) {
        TransactionResponse base = toDto(tx);
        String dir = (tx.getSourceWallet() != null
                && tx.getSourceWallet().getId().equals(walletId))
                ? "DEBIT" : "CREDIT";
        return new TransactionResponse(
                base.id(), base.transactionRef(), base.type(), base.status(),
                base.amount(), base.fee(), base.currency(), base.description(),
                base.completedAt(), base.createdAt(), dir,
                base.sourceBalanceBefore(), base.sourceBalanceAfter(),
                base.targetBalanceBefore(), base.targetBalanceAfter());
    }
}
