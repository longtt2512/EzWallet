package com.ezwallet.module.transaction.repository;

import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.transaction.entity.FeeRule;
import com.ezwallet.module.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface FeeRuleRepository extends JpaRepository<FeeRule, Long> {

    @Query("""
            SELECT f FROM FeeRule f
            WHERE f.txType = :txType
              AND f.tier = :tier
              AND f.active = true
              AND f.minAmount <= :amount
              AND f.maxAmount >= :amount
              AND (f.effectiveFrom IS NULL OR f.effectiveFrom <= :now)
              AND (f.effectiveTo IS NULL OR f.effectiveTo >= :now)
            ORDER BY f.minAmount DESC
            LIMIT 1
            """)
    Optional<FeeRule> findApplicableRule(
            @Param("txType") TransactionType txType,
            @Param("tier") UserTier tier,
            @Param("amount") BigDecimal amount,
            @Param("now") Instant now);
}
