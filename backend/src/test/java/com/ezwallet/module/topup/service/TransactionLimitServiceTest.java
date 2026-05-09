package com.ezwallet.module.topup.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.entity.WalletStatus;
import com.ezwallet.module.transaction.entity.TransactionLimit;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionLimitRepository;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionLimitService — BVA Tests")
class TransactionLimitServiceTest {

    @Mock TransactionLimitRepository limitRepository;
    @Mock TransactionRepository transactionRepository;

    @InjectMocks TransactionLimitService limitService;

    private Wallet wallet;
    private TransactionLimit bronzeLimit;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setStatus(WalletStatus.ACTIVE);

        bronzeLimit = TransactionLimit.builder()
                .tier(UserTier.BRONZE)
                .txType(TransactionType.TOPUP)
                .perTransaction(new BigDecimal("5000000"))
                .perDay(new BigDecimal("10000000"))
                .perMonth(new BigDecimal("50000000"))
                .dailyCountLimit(10)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Per-transaction BVA")
    class PerTransactionBva {

        @Test
        @DisplayName("BVA-01: amount = perTransaction limit → reject")
        void amountAtLimitBoundary_reject() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(
                    eq(UserTier.BRONZE), eq(TransactionType.TOPUP)))
                    .thenReturn(Optional.of(bronzeLimit));
            when(transactionRepository.sumCompletedAmountToday(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countCompletedToday(any(), any(), any()))
                    .thenReturn(0L);
            when(transactionRepository.sumCompletedAmountThisMonth(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            // 5,000,000 == perTransaction → should pass (not strictly greater)
            assertThatNoException().isThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            new BigDecimal("5000000")));
        }

        @Test
        @DisplayName("BVA-02: amount = perTransaction + 1 → reject")
        void amountOneOverLimit_reject() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(
                    eq(UserTier.BRONZE), eq(TransactionType.TOPUP)))
                    .thenReturn(Optional.of(bronzeLimit));

            assertThatThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            new BigDecimal("5000001")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("BVA-03: amount = 1 (well below limit) → accept")
        void amountMinimum_accept() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(
                    eq(UserTier.BRONZE), eq(TransactionType.TOPUP)))
                    .thenReturn(Optional.of(bronzeLimit));
            when(transactionRepository.sumCompletedAmountToday(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countCompletedToday(any(), any(), any()))
                    .thenReturn(0L);
            when(transactionRepository.sumCompletedAmountThisMonth(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            assertThatNoException().isThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            BigDecimal.ONE));
        }
    }

    @Nested
    @DisplayName("Daily amount BVA")
    class DailyAmountBva {

        @Test
        @DisplayName("BVA-04: dailyUsed + amount == perDay → accept")
        void exactlyAtDailyLimit_accept() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(any(), any()))
                    .thenReturn(Optional.of(bronzeLimit));
            // already used 9,000,000, now sending 1,000,000 → total 10,000,000 == perDay
            when(transactionRepository.sumCompletedAmountToday(any(), any(), any()))
                    .thenReturn(new BigDecimal("9000000"));
            when(transactionRepository.countCompletedToday(any(), any(), any()))
                    .thenReturn(0L);
            when(transactionRepository.sumCompletedAmountThisMonth(any(), any(), any()))
                    .thenReturn(new BigDecimal("9000000"));

            assertThatNoException().isThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            new BigDecimal("1000000")));
        }

        @Test
        @DisplayName("BVA-05: dailyUsed + amount > perDay by 1 → reject")
        void oneOverDailyLimit_reject() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(any(), any()))
                    .thenReturn(Optional.of(bronzeLimit));
            when(transactionRepository.sumCompletedAmountToday(any(), any(), any()))
                    .thenReturn(new BigDecimal("9000000"));

            assertThatThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            new BigDecimal("1000001")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("DAILY_LIMIT_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("Daily count BVA")
    class DailyCountBva {

        @Test
        @DisplayName("BVA-06: count == dailyCountLimit - 1 → accept")
        void countBelowLimit_accept() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(any(), any()))
                    .thenReturn(Optional.of(bronzeLimit));
            when(transactionRepository.sumCompletedAmountToday(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countCompletedToday(any(), any(), any()))
                    .thenReturn(9L); // limit is 10, so 9 < 10 → accept
            when(transactionRepository.sumCompletedAmountThisMonth(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            assertThatNoException().isThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            new BigDecimal("100000")));
        }

        @Test
        @DisplayName("BVA-07: count == dailyCountLimit → reject")
        void countAtLimit_reject() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(any(), any()))
                    .thenReturn(Optional.of(bronzeLimit));
            when(transactionRepository.sumCompletedAmountToday(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countCompletedToday(any(), any(), any()))
                    .thenReturn(10L); // == dailyCountLimit → reject

            assertThatThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.BRONZE, TransactionType.TOPUP,
                            new BigDecimal("100000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("DAILY_COUNT_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("No active limit rule")
    class NoLimitRule {

        @Test
        @DisplayName("BVA-08: no limit rule found → always accept")
        void noLimitRule_alwaysAccept() {
            when(limitRepository.findByTierAndTxTypeAndActiveTrue(any(), any()))
                    .thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() ->
                    limitService.checkLimit(wallet, UserTier.GOLD, TransactionType.TOPUP,
                            new BigDecimal("999999999")));
        }
    }
}
