package com.ezwallet.module.transaction.repository;

import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.transaction.entity.TransactionLimit;
import com.ezwallet.module.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {

    Optional<TransactionLimit> findByTierAndTxTypeAndActiveTrue(UserTier tier, TransactionType txType);
}
