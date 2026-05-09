package com.ezwallet.module.bill.repository;

import com.ezwallet.module.bill.entity.Bill;
import com.ezwallet.module.bill.entity.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillCode(String billCode);

    Optional<Bill> findByProviderIdAndCustomerCodeAndStatus(
            Long providerId, String customerCode, BillStatus status);

    Page<Bill> findByPaidByUserId(Long userId, Pageable pageable);
}
