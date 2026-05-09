package com.ezwallet.module.transaction.service;

import com.ezwallet.common.PageResponse;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.entity.WalletStatus;
import com.ezwallet.module.account.repository.WalletRepository;
import com.ezwallet.module.transaction.dto.TransactionFilterRequest;
import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.topup.dto.TransactionResponse;
import com.ezwallet.module.topup.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — EP on filters, Pagination BVA")
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock WalletRepository walletRepository;
    @Mock TransactionMapper transactionMapper;

    @InjectMocks TransactionService transactionService;

    private Wallet wallet;
    private TransactionResponse sampleDto;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setId(10L);
        wallet.setStatus(WalletStatus.ACTIVE);

        sampleDto = new TransactionResponse(1L, "TXP-ABC", TransactionType.TOPUP,
                TransactionStatus.COMPLETED, new BigDecimal("100000"), BigDecimal.ZERO,
                "VND", null, null, Instant.now());

        lenient().when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        lenient().when(transactionMapper.toDto(any())).thenReturn(sampleDto);
    }

    // ─── EP: filter equivalence partitions ──────────────────────────────────

    @Nested
    @DisplayName("EP: filter combinations")
    class FilterEp {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("EP-01: type filter only — spec has type predicate")
        void typeFilterOnly() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    TransactionType.TOPUP, null, null, null, 0, 10);
            PageResponse<TransactionResponse> result = transactionService.listTransactions(1L, filter);

            verify(transactionRepository).findAll(any(Specification.class), any(PageRequest.class));
            assertThat(result.getTotalElements()).isZero();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("EP-02: status filter only — spec has status predicate")
        void statusFilterOnly() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, TransactionStatus.COMPLETED, null, null, 0, 10);
            transactionService.listTransactions(1L, filter);

            verify(transactionRepository).findAll(any(Specification.class), any(PageRequest.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("EP-03: date range filter only — spec has dateFrom/dateTo predicates")
        void dateRangeFilterOnly() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant to = Instant.now();
            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, null, from, to, 0, 10);
            transactionService.listTransactions(1L, filter);

            verify(transactionRepository).findAll(any(Specification.class), any(PageRequest.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("EP-04: combined filters — all predicates applied")
        void combinedFilters() {
            Transaction tx = Transaction.builder()
                    .transactionRef("TXP-001")
                    .type(TransactionType.TOPUP)
                    .status(TransactionStatus.COMPLETED)
                    .amount(new BigDecimal("100000"))
                    .build();
            Page<Transaction> page = new PageImpl<>(List.of(tx));
            when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    TransactionType.TOPUP, TransactionStatus.COMPLETED,
                    Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(), 0, 10);
            PageResponse<TransactionResponse> result = transactionService.listTransactions(1L, filter);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("EP-05: no wallet for user — returns empty page immediately")
        void noWallet_returnsEmpty() {
            when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, null, null, null, 0, 10);
            PageResponse<TransactionResponse> result = transactionService.listTransactions(99L, filter);

            assertThat(result.getTotalElements()).isZero();
            verifyNoInteractions(transactionRepository);
        }
    }

    // ─── Pagination BVA ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Pagination BVA")
    class PaginationBva {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("BVA-01: page=0 size=10 → PageRequest(0, 10, DESC createdAt)")
        void firstPageCorrectPageRequest() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            when(transactionRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(emptyPage);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, null, null, null, 0, 10);
            transactionService.listTransactions(1L, filter);

            PageRequest pr = captor.getValue();
            assertThat(pr.getPageNumber()).isZero();
            assertThat(pr.getPageSize()).isEqualTo(10);
            assertThat(pr.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("BVA-02: page=1 → PageRequest(1, 10) — correct offset skips first page")
        void secondPageCorrectOffset() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 25);
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            when(transactionRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(emptyPage);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, null, null, null, 1, 10);
            PageResponse<TransactionResponse> result = transactionService.listTransactions(1L, filter);

            assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(25);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("BVA-03: size > 100 → clamped to 10 by constructor")
        void oversizePageClamped() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            when(transactionRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(emptyPage);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, null, null, null, 0, 999);
            transactionService.listTransactions(1L, filter);

            assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("BVA-04: negative page → clamped to 0 by constructor")
        void negativePageClamped() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            when(transactionRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(emptyPage);

            TransactionFilterRequest filter = new TransactionFilterRequest(
                    null, null, null, null, -5, 10);
            transactionService.listTransactions(1L, filter);

            assertThat(captor.getValue().getPageNumber()).isZero();
        }
    }
}
