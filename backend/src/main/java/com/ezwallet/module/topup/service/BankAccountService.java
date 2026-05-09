package com.ezwallet.module.topup.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.BankAccount;
import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.account.repository.BankAccountRepository;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.topup.dto.BankAccountRequest;
import com.ezwallet.module.topup.dto.BankAccountResponse;
import com.ezwallet.module.topup.mapper.BankAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final BankAccountMapper mapper;

    public List<BankAccountResponse> listByUser(Long userId) {
        return bankAccountRepository.findByUserId(userId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public BankAccountResponse link(Long userId, BankAccountRequest req) {
        if (bankAccountRepository.existsByUserIdAndBankCodeAndAccountNumber(
                userId, req.bankCode(), req.accountNumber())) {
            throw new BusinessException("BANK_ACCOUNT_EXISTS",
                    "Tài khoản ngân hàng này đã được liên kết", HttpStatus.CONFLICT);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        BankAccount account = BankAccount.builder()
                .user(user)
                .bankCode(req.bankCode())
                .accountNumber(req.accountNumber())
                .accountHolder(req.accountHolder())
                .build();
        return mapper.toDto(bankAccountRepository.save(account));
    }

    @Transactional
    public void setDefault(Long userId, Long accountId) {
        BankAccount account = findOwned(userId, accountId);
        // Clear current default
        bankAccountRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(a -> { a.setDefault(false); bankAccountRepository.save(a); });
        account.setDefault(true);
        bankAccountRepository.save(account);
    }

    @Transactional
    public void delete(Long userId, Long accountId) {
        BankAccount account = findOwned(userId, accountId);
        bankAccountRepository.delete(account);
    }

    public BankAccount findOwned(Long userId, Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND",
                        "Không tìm thấy tài khoản ngân hàng", HttpStatus.NOT_FOUND));
        if (!account.getUser().getId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED",
                    "Bạn không có quyền truy cập tài khoản này", HttpStatus.FORBIDDEN);
        }
        return account;
    }
}
