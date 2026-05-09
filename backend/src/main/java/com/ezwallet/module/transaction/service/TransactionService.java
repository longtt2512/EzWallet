package com.ezwallet.module.transaction.service;

import com.ezwallet.common.PageResponse;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.repository.WalletRepository;
import com.ezwallet.module.transaction.dto.TransactionFilterRequest;
import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.topup.dto.TransactionResponse;
import com.ezwallet.module.topup.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;

    public PageResponse<TransactionResponse> listTransactions(Long userId,
                                                               TransactionFilterRequest filter) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return PageResponse.<TransactionResponse>builder()
                    .content(List.of()).page(0).size(filter.size())
                    .totalElements(0).totalPages(0).first(true).last(true).build();
        }

        final Long walletId = wallet.getId();

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Transactions belonging to this wallet (source or target)
            predicates.add(cb.or(
                    cb.equal(root.get("sourceWallet").get("id"), walletId),
                    cb.equal(root.get("targetWallet").get("id"), walletId)
            ));

            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.dateFrom()));
            }
            if (filter.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.dateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(filter.page(), filter.size(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<TransactionResponse> page = transactionRepository
                .findAll(spec, pageRequest)
                .map(tx -> transactionMapper.toDto(tx, walletId));

        return PageResponse.from(page);
    }
}
