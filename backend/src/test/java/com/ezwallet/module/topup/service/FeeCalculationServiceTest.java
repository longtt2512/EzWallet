package com.ezwallet.module.topup.service;

import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.transaction.entity.FeeRule;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.FeeRuleRepository;
import com.ezwallet.module.transaction.service.FeeCalculationService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeeCalculationService — Decision Table Tests")
class FeeCalculationServiceTest {

    @Mock
    FeeRuleRepository feeRuleRepository;

    @InjectMocks
    FeeCalculationService feeCalculationService;

    private FeeRule buildFixedRule(BigDecimal feeValue) {
        return FeeRule.builder()
                .txType(TransactionType.WITHDRAW)
                .tier(UserTier.BRONZE)
                .minAmount(BigDecimal.ZERO)
                .maxAmount(new BigDecimal("999999999"))
                .feeType("FIXED")
                .feeValue(feeValue)
                .active(true)
                .build();
    }

    private FeeRule buildPercentRule(BigDecimal percent, BigDecimal feeMin, BigDecimal feeMax) {
        return FeeRule.builder()
                .txType(TransactionType.WITHDRAW)
                .tier(UserTier.GOLD)
                .minAmount(BigDecimal.ZERO)
                .maxAmount(new BigDecimal("999999999"))
                .feeType("PERCENT")
                .feeValue(percent)
                .feeMin(feeMin)
                .feeMax(feeMax)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("FIXED fee rules")
    class FixedFeeRules {

        @Test
        @DisplayName("R1 — BRONZE WITHDRAW: fixed 5000 VND fee")
        void bronzeWithdrawFixedFee() {
            FeeRule rule = buildFixedRule(new BigDecimal("5000"));
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.WITHDRAW), eq(UserTier.BRONZE),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.of(rule));

            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.WITHDRAW, UserTier.BRONZE, new BigDecimal("100000"));

            assertThat(fee).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("R2 — SILVER WITHDRAW: fixed 5000 VND fee")
        void silverWithdrawFixedFee() {
            FeeRule rule = FeeRule.builder()
                    .txType(TransactionType.WITHDRAW).tier(UserTier.SILVER)
                    .feeType("FIXED").feeValue(new BigDecimal("5000"))
                    .minAmount(BigDecimal.ZERO).maxAmount(new BigDecimal("999999999"))
                    .active(true).build();
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.WITHDRAW), eq(UserTier.SILVER),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.of(rule));

            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.WITHDRAW, UserTier.SILVER, new BigDecimal("500000"));

            assertThat(fee).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("R3 — GOLD WITHDRAW: fixed 0 VND fee")
        void goldWithdrawZeroFee() {
            FeeRule rule = FeeRule.builder()
                    .txType(TransactionType.WITHDRAW).tier(UserTier.GOLD)
                    .feeType("FIXED").feeValue(BigDecimal.ZERO)
                    .minAmount(BigDecimal.ZERO).maxAmount(new BigDecimal("999999999"))
                    .active(true).build();
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.WITHDRAW), eq(UserTier.GOLD),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.of(rule));

            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.WITHDRAW, UserTier.GOLD, new BigDecimal("1000000"));

            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("R4 — TOPUP any tier: no fee (no rule)")
        void topupNoFeeWhenNoRule() {
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.TOPUP), any(UserTier.class),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.empty());

            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.TOPUP, UserTier.BRONZE, new BigDecimal("500000"));

            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("PERCENT fee rules")
    class PercentFeeRules {

        @Test
        @DisplayName("R5 — 0.5% fee on 1000000 = 5000")
        void percentFeeCalculation() {
            FeeRule rule = buildPercentRule(new BigDecimal("0.5"), null, null);
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.WITHDRAW), eq(UserTier.GOLD),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.of(rule));

            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.WITHDRAW, UserTier.GOLD, new BigDecimal("1000000"));

            assertThat(fee).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("R6 — percent fee capped at feeMax")
        void percentFeeCappedAtMax() {
            FeeRule rule = buildPercentRule(new BigDecimal("0.5"),
                    new BigDecimal("1000"), new BigDecimal("50000"));
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.WITHDRAW), eq(UserTier.GOLD),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.of(rule));

            // 0.5% of 20,000,000 = 100,000 but capped at 50,000
            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.WITHDRAW, UserTier.GOLD, new BigDecimal("20000000"));

            assertThat(fee).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("R7 — percent fee raised to feeMin")
        void percentFeeRaisedToMin() {
            FeeRule rule = buildPercentRule(new BigDecimal("0.5"),
                    new BigDecimal("3000"), new BigDecimal("50000"));
            when(feeRuleRepository.findApplicableRule(
                    eq(TransactionType.WITHDRAW), eq(UserTier.GOLD),
                    any(BigDecimal.class), any(Instant.class)))
                    .thenReturn(Optional.of(rule));

            // 0.5% of 200 = 1, raised to feeMin 3000
            BigDecimal fee = feeCalculationService.calculate(
                    TransactionType.WITHDRAW, UserTier.GOLD, new BigDecimal("200"));

            assertThat(fee).isEqualByComparingTo(new BigDecimal("3000"));
        }
    }
}
