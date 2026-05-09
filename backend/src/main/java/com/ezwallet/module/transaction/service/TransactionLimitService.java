package com.ezwallet.module.transaction.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.transaction.entity.TransactionLimit;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionLimitRepository;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionLimitService {

    private final TransactionLimitRepository limitRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Kiểm tra hạn mức giao dịch. Ném BusinessException nếu vượt quá.
     */
    public void checkLimit(Wallet wallet, UserTier tier, TransactionType txType, BigDecimal amount) {
        Optional<TransactionLimit> limitOpt = limitRepository.findByTierAndTxTypeAndActiveTrue(tier, txType);
        if (limitOpt.isEmpty()) return;

        TransactionLimit limit = limitOpt.get();

        // Per-transaction limit
        if (amount.compareTo(limit.getPerTransaction()) > 0) {
            throw new BusinessException("LIMIT_EXCEEDED",
                    "Số tiền vượt quá hạn mức mỗi giao dịch (" + limit.getPerTransaction() + " VND)",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant dayStart = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant();
        Instant monthStart = ZonedDateTime.now(zone).toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant();

        // Daily amount limit
        BigDecimal dailyUsed = transactionRepository.sumCompletedAmountToday(wallet.getId(), txType, dayStart);
        if (dailyUsed.add(amount).compareTo(limit.getPerDay()) > 0) {
            throw new BusinessException("DAILY_LIMIT_EXCEEDED",
                    "Vượt quá hạn mức giao dịch trong ngày (" + limit.getPerDay() + " VND)",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Daily count limit
        if (limit.getDailyCountLimit() != null) {
            long dailyCount = transactionRepository.countCompletedToday(wallet.getId(), txType, dayStart);
            if (dailyCount >= limit.getDailyCountLimit()) {
                throw new BusinessException("DAILY_COUNT_EXCEEDED",
                        "Vượt quá số lần giao dịch trong ngày (" + limit.getDailyCountLimit() + " lần)",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // Monthly amount limit
        BigDecimal monthlyUsed = transactionRepository.sumCompletedAmountThisMonth(wallet.getId(), txType, monthStart);
        if (monthlyUsed.add(amount).compareTo(limit.getPerMonth()) > 0) {
            throw new BusinessException("MONTHLY_LIMIT_EXCEEDED",
                    "Vượt quá hạn mức giao dịch trong tháng (" + limit.getPerMonth() + " VND)",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
