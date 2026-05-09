package com.ezwallet.module.account.repository;

import com.ezwallet.module.account.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByUserId(Long userId);

    Optional<BankAccount> findByUserIdAndIsDefaultTrue(Long userId);

    boolean existsByUserIdAndBankCodeAndAccountNumber(Long userId, String bankCode, String accountNumber);
}
