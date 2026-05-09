package com.ezwallet.module.bill.repository;

import com.ezwallet.module.bill.entity.BillProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillProviderRepository extends JpaRepository<BillProvider, Long> {

    List<BillProvider> findByActiveTrue();

    Optional<BillProvider> findByCode(String code);
}
