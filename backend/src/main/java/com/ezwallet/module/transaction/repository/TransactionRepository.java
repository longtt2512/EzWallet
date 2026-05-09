package com.ezwallet.module.transaction.repository;

import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByTransactionRef(String transactionRef);

    Page<Transaction> findBySourceWalletId(Long walletId, Pageable pageable);

    Page<Transaction> findByTargetWalletId(Long walletId, Pageable pageable);

    /** Sum of completed transactions of a given type for a wallet today */
    @Query("""
            SELECT COALESCE(SUM(t.amount + t.fee), 0)
            FROM Transaction t
            WHERE t.sourceWallet.id = :walletId
              AND t.type = :type
              AND t.status = 'COMPLETED'
              AND t.completedAt >= :dayStart
            """)
    BigDecimal sumCompletedAmountToday(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("dayStart") Instant dayStart);

    /** Count of completed transactions of a given type for a wallet today */
    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.sourceWallet.id = :walletId
              AND t.type = :type
              AND t.status = 'COMPLETED'
              AND t.completedAt >= :dayStart
            """)
    long countCompletedToday(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("dayStart") Instant dayStart);

    /** Sum of completed transactions of a given type for a wallet this month */
    @Query("""
            SELECT COALESCE(SUM(t.amount + t.fee), 0)
            FROM Transaction t
            WHERE t.sourceWallet.id = :walletId
              AND t.type = :type
              AND t.status = 'COMPLETED'
              AND t.completedAt >= :monthStart
            """)
    BigDecimal sumCompletedAmountThisMonth(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("monthStart") Instant monthStart);
}
