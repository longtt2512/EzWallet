package com.ezwallet.module.topup.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.entity.WalletStatus;
import com.ezwallet.module.account.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND",
                        "Không tìm thấy ví", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Wallet creditWallet(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND",
                        "Không tìm thấy ví", HttpStatus.NOT_FOUND));
        requireActive(wallet);
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debitWallet(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND",
                        "Không tìm thấy ví", HttpStatus.NOT_FOUND));
        requireActive(wallet);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE",
                    "Số dư ví không đủ", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        return walletRepository.save(wallet);
    }

    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException("WALLET_NOT_ACTIVE",
                    "Ví không ở trạng thái hoạt động", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
