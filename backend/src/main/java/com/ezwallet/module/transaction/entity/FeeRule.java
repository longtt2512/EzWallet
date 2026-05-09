package com.ezwallet.module.transaction.entity;

import com.ezwallet.common.BaseEntity;
import com.ezwallet.module.account.entity.UserTier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fee_rules",
        indexes = @Index(name = "idx_fee_rules_lookup", columnList = "tx_type, tier, active"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 30)
    private TransactionType txType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserTier tier;

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    /** FIXED hoặc PERCENT */
    @Column(name = "fee_type", nullable = false, length = 20)
    private String feeType;

    @Column(name = "fee_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeValue;

    @Column(name = "fee_min", precision = 19, scale = 2)
    private BigDecimal feeMin;

    @Column(name = "fee_max", precision = 19, scale = 2)
    private BigDecimal feeMax;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;
}
