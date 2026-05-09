package com.ezwallet.module.transaction.service;

import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.transaction.entity.FeeRule;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.FeeRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeeCalculationService {

    private final FeeRuleRepository feeRuleRepository;

    /**
     * Tính phí giao dịch dựa trên fee_rules.
     * Trả về 0 nếu không tìm thấy rule phù hợp.
     */
    public BigDecimal calculate(TransactionType txType, UserTier tier, BigDecimal amount) {
        Optional<FeeRule> ruleOpt = feeRuleRepository.findApplicableRule(txType, tier, amount, Instant.now());
        if (ruleOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        FeeRule rule = ruleOpt.get();
        BigDecimal fee = switch (rule.getFeeType()) {
            case "FIXED" -> rule.getFeeValue();
            case "PERCENT" -> amount.multiply(rule.getFeeValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };
        // Apply min/max caps
        if (rule.getFeeMin() != null && fee.compareTo(rule.getFeeMin()) < 0) {
            fee = rule.getFeeMin();
        }
        if (rule.getFeeMax() != null && fee.compareTo(rule.getFeeMax()) > 0) {
            fee = rule.getFeeMax();
        }
        return fee;
    }
}
