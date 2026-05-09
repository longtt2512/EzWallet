package com.ezwallet.module.transfer.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.*;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.service.OtpService;
import com.ezwallet.module.transaction.entity.*;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.FeeCalculationService;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import com.ezwallet.module.topup.service.WalletService;
import com.ezwallet.module.transfer.dto.P2PTransferRequest;
import com.ezwallet.module.transfer.dto.QrGenerateRequest;
import com.ezwallet.module.transfer.dto.QrPayRequest;
import com.ezwallet.module.transfer.dto.TransferResponse;
import com.ezwallet.module.transfer.mapper.TransferMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService — BVA, State Transition, Decision Table, Idempotency Tests")
class TransferServiceTest {

    @Mock WalletService walletService;
    @Mock UserRepository userRepository;
    @Mock OtpService otpService;
    @Mock FeeCalculationService feeCalculationService;
    @Mock TransactionLimitService transactionLimitService;
    @Mock TransactionRepository transactionRepository;
    @Mock TransferMapper transferMapper;
    @Mock QrCodeService qrCodeService;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;

    @InjectMocks TransferService transferService;

    private User sender;
    private User receiver;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("alice");
        sender.setTier(UserTier.BRONZE);

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("bob");
        receiver.setTier(UserTier.BRONZE);

        senderWallet = new Wallet();
        senderWallet.setId(10L);
        senderWallet.setBalance(new BigDecimal("1000000"));
        senderWallet.setStatus(WalletStatus.ACTIVE);
        senderWallet.setUser(sender);

        receiverWallet = new Wallet();
        receiverWallet.setId(20L);
        receiverWallet.setBalance(BigDecimal.ZERO);
        receiverWallet.setStatus(WalletStatus.ACTIVE);
        receiverWallet.setUser(receiver);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transferMapper.toDto(any())).thenReturn(
                new TransferResponse(null, null, TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    }

    // ─── P2P transfer tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("p2pTransfer — BVA on fee threshold")
    class P2pFeeBva {

        @BeforeEach
        void stubCommon() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByUsername("bob")).thenReturn(Optional.of(receiver));
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.TRANSFER), anyString());
            when(walletService.getWallet(1L)).thenReturn(senderWallet);
            when(walletService.getWallet(2L)).thenReturn(receiverWallet);
            when(walletService.debitWallet(anyLong(), any())).thenReturn(senderWallet);
            when(walletService.creditWallet(anyLong(), any())).thenReturn(receiverWallet);
        }

        @Test
        @DisplayName("BVA-01: BRONZE amount < free threshold → fee=0")
        void bronzeAmountBelowFreeThreshold_noFee() {
            // BRONZE free threshold: 500,000 VND
            BigDecimal amount = new BigDecimal("499999");
            when(feeCalculationService.calculate(TransactionType.TRANSFER, UserTier.BRONZE, amount))
                    .thenReturn(BigDecimal.ZERO);

            P2PTransferRequest req = new P2PTransferRequest("bob", amount, null, "123456", null);
            transferService.p2pTransfer(1L, req);

            verify(walletService).debitWallet(eq(senderWallet.getId()), eq(amount));
        }

        @Test
        @DisplayName("BVA-02: BRONZE amount > free threshold → fee=0.5% applied")
        void bronzeAmountAboveFreeThreshold_feeApplied() {
            BigDecimal amount = new BigDecimal("500001");
            BigDecimal fee = amount.multiply(new BigDecimal("0.5"))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            when(feeCalculationService.calculate(TransactionType.TRANSFER, UserTier.BRONZE, amount))
                    .thenReturn(fee);

            P2PTransferRequest req = new P2PTransferRequest("bob", amount, null, "123456", null);
            transferService.p2pTransfer(1L, req);

            verify(walletService).debitWallet(eq(senderWallet.getId()), eq(amount.add(fee)));
        }
    }

    @Nested
    @DisplayName("p2pTransfer — Self-transfer rejection")
    class SelfTransfer {

        @Test
        @DisplayName("BVA-03: sender == receiver → SELF_TRANSFER exception")
        void selfTransfer_rejected() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

            P2PTransferRequest req = new P2PTransferRequest("alice", new BigDecimal("50000"),
                    null, "123456", null);
            assertThatThrownBy(() -> transferService.p2pTransfer(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("SELF_TRANSFER");
        }
    }

    @Nested
    @DisplayName("p2pTransfer — Idempotency")
    class Idempotency {

        @Test
        @DisplayName("IT-01: duplicate idempotency key → returns existing tx, no second debit")
        void duplicateIdempotencyKey_noDoubleDebit() {
            Transaction existing = Transaction.builder()
                    .idempotencyKey("idem-001")
                    .status(TransactionStatus.COMPLETED)
                    .build();
            when(transactionRepository.findByIdempotencyKey("idem-001"))
                    .thenReturn(Optional.of(existing));

            P2PTransferRequest req = new P2PTransferRequest("bob", new BigDecimal("50000"),
                    null, "123456", "idem-001");
            transferService.p2pTransfer(1L, req);

            verify(walletService, never()).debitWallet(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("p2pTransfer — State Transition: PENDING → COMPLETED / FAILED")
    class StateTransition {

        @BeforeEach
        void stubCommon() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findByUsername("bob")).thenReturn(Optional.of(receiver));
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.TRANSFER), anyString());
            when(walletService.getWallet(1L)).thenReturn(senderWallet);
            when(walletService.getWallet(2L)).thenReturn(receiverWallet);
            when(feeCalculationService.calculate(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            lenient().when(walletService.creditWallet(anyLong(), any())).thenReturn(receiverWallet);
        }

        @Test
        @DisplayName("ST-01: PENDING → COMPLETED on success")
        void pendingToCompleted() {
            when(walletService.debitWallet(anyLong(), any())).thenReturn(senderWallet);

            P2PTransferRequest req = new P2PTransferRequest("bob", new BigDecimal("100000"),
                    null, "123456", null);
            transferService.p2pTransfer(1L, req);

            verify(transactionRepository, atLeastOnce()).save(argThat(tx ->
                    ((Transaction) tx).getStatus() == TransactionStatus.COMPLETED));
        }

        @Test
        @DisplayName("ST-02: PENDING → FAILED on insufficient balance")
        void pendingToFailed_insufficientBalance() {
            when(walletService.debitWallet(anyLong(), any()))
                    .thenThrow(new BusinessException("INSUFFICIENT_BALANCE",
                            "Số dư không đủ", HttpStatus.UNPROCESSABLE_ENTITY));

            P2PTransferRequest req = new P2PTransferRequest("bob", new BigDecimal("100000"),
                    null, "123456", null);
            assertThatThrownBy(() -> transferService.p2pTransfer(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("INSUFFICIENT_BALANCE");

            verify(transactionRepository, atLeastOnce()).save(argThat(tx ->
                    ((Transaction) tx).getStatus() == TransactionStatus.FAILED));
        }
    }

    @Nested
    @DisplayName("payQr — QR state tests")
    class QrStateTests {

        @Test
        @DisplayName("QR-01: expired / missing QR → EXPIRED_QR exception")
        void expiredQr_throws() {
            when(valueOps.get("qr:nonexistent")).thenReturn(null);

            QrPayRequest req = new QrPayRequest("nonexistent", "123456", null);
            assertThatThrownBy(() -> transferService.payQr(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("EXPIRED_QR");
        }

        @Test
        @DisplayName("QR-02: already-paid QR → QR_ALREADY_PAID exception")
        void alreadyPaidQr_throws() {
            Map<String, Object> meta = new HashMap<>();
            meta.put("receiverId", 2L);
            meta.put("amount", "100000");
            meta.put("note", "");
            meta.put("expiresAt", Instant.now().plus(10, ChronoUnit.MINUTES).toString());
            meta.put("paid", true);
            when(valueOps.get("qr:paid-qr")).thenReturn(meta);

            QrPayRequest req = new QrPayRequest("paid-qr", "123456", null);
            assertThatThrownBy(() -> transferService.payQr(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("QR_ALREADY_PAID");
        }

        @Test
        @DisplayName("QR-03: valid unpaid QR → executes transfer")
        void validQr_executesTransfer() {
            Map<String, Object> meta = new HashMap<>();
            meta.put("receiverId", 2L);
            meta.put("amount", "100000");
            meta.put("note", "test");
            meta.put("expiresAt", Instant.now().plus(10, ChronoUnit.MINUTES).toString());
            meta.put("paid", false);
            when(valueOps.get("qr:valid-qr")).thenReturn(meta);

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.TRANSFER), anyString());
            when(walletService.getWallet(1L)).thenReturn(senderWallet);
            when(walletService.getWallet(2L)).thenReturn(receiverWallet);
            when(feeCalculationService.calculate(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(walletService.debitWallet(anyLong(), any())).thenReturn(senderWallet);
            when(walletService.creditWallet(anyLong(), any())).thenReturn(receiverWallet);

            QrPayRequest req = new QrPayRequest("valid-qr", "123456", null);
            assertThatNoException().isThrownBy(() -> transferService.payQr(1L, req));

            verify(walletService).debitWallet(eq(senderWallet.getId()), eq(new BigDecimal("100000")));
            verify(walletService).creditWallet(eq(receiverWallet.getId()), eq(new BigDecimal("100000")));
        }
    }

    @Nested
    @DisplayName("Decision table: fee rules for TRANSFER (tier × amount)")
    class TransferFeeDecisionTable {

        @BeforeEach
        void stubCommon() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            lenient().when(userRepository.findByUsername("bob")).thenReturn(Optional.of(receiver));
            lenient().doNothing().when(otpService).verify(any(), eq(OtpPurpose.TRANSFER), anyString());
            lenient().when(walletService.getWallet(1L)).thenReturn(senderWallet);
            lenient().when(walletService.getWallet(2L)).thenReturn(receiverWallet);
            lenient().when(walletService.debitWallet(anyLong(), any())).thenReturn(senderWallet);
            lenient().when(walletService.creditWallet(anyLong(), any())).thenReturn(receiverWallet);
        }

        @Test
        @DisplayName("DT-01: GOLD tier → always 0 fee regardless of amount")
        void goldTierZeroFee() {
            sender.setTier(UserTier.GOLD);
            BigDecimal amount = new BigDecimal("5000000");
            when(feeCalculationService.calculate(TransactionType.TRANSFER, UserTier.GOLD, amount))
                    .thenReturn(BigDecimal.ZERO);

            P2PTransferRequest req = new P2PTransferRequest("bob", amount, null, "123456", null);
            transferService.p2pTransfer(1L, req);

            verify(walletService).debitWallet(eq(senderWallet.getId()), eq(amount));
        }

        @Test
        @DisplayName("DT-02: receiver not found → RECEIVER_NOT_FOUND exception")
        void receiverNotFound_throws() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByPhone("unknown")).thenReturn(Optional.empty());

            P2PTransferRequest req = new P2PTransferRequest("unknown", new BigDecimal("50000"),
                    null, "123456", null);
            assertThatThrownBy(() -> transferService.p2pTransfer(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("RECEIVER_NOT_FOUND");
        }
    }
}
