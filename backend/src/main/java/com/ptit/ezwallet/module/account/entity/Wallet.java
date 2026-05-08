package com.ptit.ezwallet.module.account.entity;

import com.ptit.ezwallet.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ví của người dùng (1-1 với User).
 * Mỗi user có duy nhất 1 ví, được tạo tự động khi tài khoản chuyển sang ACTIVE.
 */
@Entity
@Table(name = "wallets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallets_user", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wallets_user"))
    private User user;

    /** Số dư ví, đơn vị VND */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;
}
