package com.ezwallet.module.topup.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.BankAccount;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.account.repository.WalletRepository;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.service.OtpService;
import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.FeeCalculationService;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import com.ezwallet.module.topup.dto.TopupRequest;
import com.ezwallet.module.topup.dto.TransactionResponse;
import com.ezwallet.module.topup.dto.WithdrawRequest;
import com.ezwallet.module.topup.mapper.TransactionMapper;
import com.ezwallet.util.IdempotencyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.ezwallet.module.account.entity.User;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TopupService {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final BankAccountService bankAccountService;
    private final FeeCalculationService feeCalculationService;
    private final TransactionLimitService transactionLimitService;
    private final TransactionRepository transactionRepository;
    private final OtpService otpService;
    private final TransactionMapper transactionMapper;

    public void sendDepositOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        String code = otpService.generate(user, user.getEmail(), OtpPurpose.TOPUP);
        otpService.sendEmail(user.getEmail(), code, OtpPurpose.TOPUP);
    }

    @Transactional
    public TransactionResponse deposit(Long userId, TopupRequest req) {
        if (req.idempotencyKey() != null) {
            var existing = transactionRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) return transactionMapper.toDto(existing.get());
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        otpService.verify(user, OtpPurpose.TOPUP, req.otpCode());

        Wallet wallet = walletService.getWallet(userId);
        BankAccount bankAccount = bankAccountService.findOwned(userId, req.bankAccountId());

        transactionLimitService.checkLimit(wallet, wallet.getUser().getTier(), TransactionType.TOPUP, req.amount());

        String idempotencyKey = req.idempotencyKey() != null
                ? req.idempotencyKey()
                : IdempotencyUtil.generateIdempotencyKey(userId, TransactionType.TOPUP, req.amount());

        BigDecimal balanceBefore = wallet.getBalance();

        Transaction tx = Transaction.builder()
                .transactionRef(IdempotencyUtil.generateTxRef())
                .type(TransactionType.TOPUP)
                .status(TransactionStatus.PENDING)
                .amount(req.amount())
                .fee(BigDecimal.ZERO)
                .targetWallet(wallet)
                .bankAccount(bankAccount)
                .idempotencyKey(idempotencyKey)
                .description("Nạp tiền từ tài khoản " + bankAccount.getBankCode())
                .targetBalanceBefore(balanceBefore)
                .build();
        transactionRepository.save(tx);

        try {
            Wallet updated = walletService.creditWallet(wallet.getId(), req.amount());
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setCompletedAt(Instant.now());
            tx.setTargetBalanceAfter(updated.getBalance());
        } catch (Exception e) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(e.getMessage());
            transactionRepository.save(tx);
            throw e;
        }
        return transactionMapper.toDto(transactionRepository.save(tx));
    }

    public void sendWithdrawOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        String code = otpService.generate(user, user.getEmail(), OtpPurpose.WITHDRAW);
        otpService.sendEmail(user.getEmail(), code, OtpPurpose.WITHDRAW);
    }

    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest req) {
        if (req.idempotencyKey() != null) {
            var existing = transactionRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) return transactionMapper.toDto(existing.get());
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        otpService.verify(user, OtpPurpose.WITHDRAW, req.otpCode());

        Wallet wallet = walletService.getWallet(userId);
        BankAccount bankAccount = bankAccountService.findOwned(userId, req.bankAccountId());

        BigDecimal fee = feeCalculationService.calculate(TransactionType.WITHDRAW, wallet.getUser().getTier(), req.amount());
        BigDecimal total = req.amount().add(fee);

        transactionLimitService.checkLimit(wallet, wallet.getUser().getTier(), TransactionType.WITHDRAW, req.amount());

        String idempotencyKey = req.idempotencyKey() != null
                ? req.idempotencyKey()
                : IdempotencyUtil.generateIdempotencyKey(userId, TransactionType.WITHDRAW, req.amount());

        BigDecimal balanceBefore = wallet.getBalance();

        Transaction tx = Transaction.builder()
                .transactionRef(IdempotencyUtil.generateTxRef())
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.PENDING)
                .amount(req.amount())
                .fee(fee)
                .sourceWallet(wallet)
                .bankAccount(bankAccount)
                .idempotencyKey(idempotencyKey)
                .description("Rút tiền về tài khoản " + bankAccount.getBankCode())
                .sourceBalanceBefore(balanceBefore)
                .build();
        transactionRepository.save(tx);

        try {
            Wallet updated = walletService.debitWallet(wallet.getId(), total);
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setCompletedAt(Instant.now());
            tx.setSourceBalanceAfter(updated.getBalance());
        } catch (Exception e) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(e.getMessage());
            transactionRepository.save(tx);
            throw e;
        }
        return transactionMapper.toDto(transactionRepository.save(tx));
    }
}
