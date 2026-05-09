package com.ezwallet.module.bill.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.entity.WalletStatus;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.bill.dto.BillPayRequest;
import com.ezwallet.module.bill.dto.BillResponse;
import com.ezwallet.module.bill.entity.Bill;
import com.ezwallet.module.bill.entity.BillProvider;
import com.ezwallet.module.bill.entity.BillStatus;
import com.ezwallet.module.bill.mapper.BillMapper;
import com.ezwallet.module.bill.repository.BillRepository;
import com.ezwallet.module.bill.repository.BillProviderRepository;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import com.ezwallet.module.topup.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillService — Decision Table, BVA, State Transition Tests")
class BillServiceTest {

    @Mock BillProviderRepository billProviderRepository;
    @Mock BillRepository billRepository;
    @Mock UserRepository userRepository;
    @Mock WalletService walletService;
    @Mock TransactionRepository transactionRepository;
    @Mock TransactionLimitService transactionLimitService;
    @Mock BillMapper billMapper;

    @InjectMocks BillService billService;

    private User user;
    private Wallet wallet;
    private BillProvider provider;
    private Bill unpaidBill;
    private BillResponse billResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setTier(UserTier.BRONZE);

        wallet = new Wallet();
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("500000"));
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUser(user);

        provider = BillProvider.builder()
                .id(1L).code("EVN").name("Điện lực").serviceType("ELECTRICITY").active(true).build();

        unpaidBill = Bill.builder()
                .id(1L)
                .billCode("BILL-001")
                .provider(provider)
                .customerCode("KH123")
                .amount(new BigDecimal("150000"))
                .status(BillStatus.UNPAID)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        billResponse = new BillResponse(1L, "BILL-001", "Điện lực", "EVN",
                "KH123", new BigDecimal("150000"), BillStatus.PAID, null);
    }

    // ─── Decision Table: bill status determines payability ──────────────────

    @Nested
    @DisplayName("Decision Table: payBill — bill status matrix")
    class BillStatusDecisionTable {

        @Test
        @DisplayName("DT-01: UNPAID bill → payment accepted")
        void unpaidBill_accepted() {
            when(billRepository.findByBillCode("BILL-001")).thenReturn(Optional.of(unpaidBill));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(walletService.getWallet(1L)).thenReturn(wallet);
            when(walletService.debitWallet(anyLong(), any())).thenReturn(wallet);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(billMapper.toDto(any(Bill.class))).thenReturn(billResponse);

            BillPayRequest req = new BillPayRequest("BILL-001", null, null);
            BillResponse result = billService.payBill(1L, req);

            assertThat(result).isNotNull();
            verify(walletService).debitWallet(eq(wallet.getId()), eq(new BigDecimal("150000")));
        }

        @ParameterizedTest(name = "DT-{index}: {0} bill → BILL_NOT_PAYABLE exception")
        @EnumSource(value = BillStatus.class, names = {"PAID", "EXPIRED", "CANCELLED"})
        @DisplayName("DT-02/03/04: PAID/EXPIRED/CANCELLED bill → rejected")
        void nonPayableBill_rejected(BillStatus status) {
            unpaidBill.setStatus(status);
            when(billRepository.findByBillCode("BILL-001")).thenReturn(Optional.of(unpaidBill));

            BillPayRequest req = new BillPayRequest("BILL-001", null, null);
            assertThatThrownBy(() -> billService.payBill(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("BILL_NOT_PAYABLE");
        }
    }

    // ─── State Transition: UNPAID → PAID ────────────────────────────────────

    @Nested
    @DisplayName("State Transition: Bill UNPAID → PAID")
    class BillStateTransition {

        @Test
        @DisplayName("ST-01: UNPAID → PAID after successful payment")
        void unpaidToPaid_onSuccess() {
            when(billRepository.findByBillCode("BILL-001")).thenReturn(Optional.of(unpaidBill));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(walletService.getWallet(1L)).thenReturn(wallet);
            when(walletService.debitWallet(anyLong(), any())).thenReturn(wallet);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(billMapper.toDto(any(Bill.class))).thenReturn(billResponse);

            billService.payBill(1L, new BillPayRequest("BILL-001", null, null));

            verify(billRepository).save(argThat(b ->
                    ((Bill) b).getStatus() == BillStatus.PAID));
        }

        @Test
        @DisplayName("ST-02: UNPAID → remains UNPAID (wallet fails, tx saved as FAILED)")
        void unpaidToFailed_walletError() {
            when(billRepository.findByBillCode("BILL-001")).thenReturn(Optional.of(unpaidBill));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(walletService.getWallet(1L)).thenReturn(wallet);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletService.debitWallet(anyLong(), any()))
                    .thenThrow(new BusinessException("INSUFFICIENT_BALANCE",
                            "Số dư không đủ", HttpStatus.UNPROCESSABLE_ENTITY));

            assertThatThrownBy(() -> billService.payBill(1L,
                    new BillPayRequest("BILL-001", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("INSUFFICIENT_BALANCE");

            // Bill was NOT set to PAID
            verify(billRepository, never()).save(argThat(b ->
                    ((Bill) b).getStatus() == BillStatus.PAID));

            // Transaction saved as FAILED
            verify(transactionRepository, atLeastOnce()).save(argThat(tx ->
                    com.ezwallet.module.transaction.entity.Transaction.class.isInstance(tx) &&
                    ((com.ezwallet.module.transaction.entity.Transaction) tx).getStatus()
                            == TransactionStatus.FAILED));
        }
    }

    // ─── BVA: bill not found ─────────────────────────────────────────────────

    @Nested
    @DisplayName("BVA: bill lookup boundaries")
    class BillLookupBva {

        @Test
        @DisplayName("BVA-01: bill code not found → BILL_NOT_FOUND exception")
        void billNotFound_throws() {
            when(billRepository.findByBillCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billService.payBill(1L,
                    new BillPayRequest("UNKNOWN", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("BILL_NOT_FOUND");
        }

        @Test
        @DisplayName("BVA-02: idempotency key exists → returns bill without double charge")
        void idempotencyKey_noDoubleCharge() {
            var existingTx = com.ezwallet.module.transaction.entity.Transaction.builder()
                    .idempotencyKey("idem-pay-001")
                    .status(TransactionStatus.COMPLETED)
                    .build();
            when(transactionRepository.findByIdempotencyKey("idem-pay-001"))
                    .thenReturn(Optional.of(existingTx));
            when(billRepository.findByBillCode("BILL-001")).thenReturn(Optional.of(unpaidBill));
            when(billMapper.toDto(unpaidBill)).thenReturn(billResponse);

            BillPayRequest req = new BillPayRequest("BILL-001", null, "idem-pay-001");
            billService.payBill(1L, req);

            verify(walletService, never()).debitWallet(anyLong(), any());
        }
    }
}
