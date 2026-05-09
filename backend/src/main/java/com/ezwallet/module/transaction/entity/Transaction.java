package com.ezwallet.module.transaction.entity;

import com.ezwallet.common.BaseEntity;
import com.ezwallet.module.account.entity.BankAccount;
import com.ezwallet.module.account.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_tx_source", columnList = "source_wallet_id, created_at"),
                @Index(name = "idx_tx_target", columnList = "target_wallet_id, created_at"),
                @Index(name = "idx_tx_status", columnList = "status"),
                @Index(name = "idx_tx_type",   columnList = "type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 50)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id", foreignKey = @ForeignKey(name = "fk_tx_source_wallet"))
    private Wallet sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_wallet_id", foreignKey = @ForeignKey(name = "fk_tx_target_wallet"))
    private Wallet targetWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", foreignKey = @ForeignKey(name = "fk_tx_bank_account"))
    private BankAccount bankAccount;

    @Column(length = 500)
    private String description;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "source_balance_before", precision = 19, scale = 2)
    private BigDecimal sourceBalanceBefore;

    @Column(name = "source_balance_after", precision = 19, scale = 2)
    private BigDecimal sourceBalanceAfter;

    @Column(name = "target_balance_before", precision = 19, scale = 2)
    private BigDecimal targetBalanceBefore;

    @Column(name = "target_balance_after", precision = 19, scale = 2)
    private BigDecimal targetBalanceAfter;
}
