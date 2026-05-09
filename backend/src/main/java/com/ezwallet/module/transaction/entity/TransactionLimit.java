package com.ezwallet.module.transaction.entity;

import com.ezwallet.common.BaseEntity;
import com.ezwallet.module.account.entity.UserTier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_limits",
        uniqueConstraints = @UniqueConstraint(name = "uk_tx_limit_tier_type", columnNames = {"tier", "tx_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLimit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 30)
    private TransactionType txType;

    @Column(name = "per_transaction", nullable = false, precision = 19, scale = 2)
    private BigDecimal perTransaction;

    @Column(name = "per_day", nullable = false, precision = 19, scale = 2)
    private BigDecimal perDay;

    @Column(name = "per_month", nullable = false, precision = 19, scale = 2)
    private BigDecimal perMonth;

    @Column(name = "daily_count_limit")
    private Integer dailyCountLimit;

    @Column(nullable = false)
    private boolean active;
}
