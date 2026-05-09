package com.ezwallet.module.topup.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.*;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.account.repository.WalletRepository;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.service.OtpService;
import com.ezwallet.module.transaction.entity.*;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.FeeCalculationService;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import com.ezwallet.module.topup.dto.TopupRequest;
import com.ezwallet.module.topup.dto.TransactionResponse;
import com.ezwallet.module.topup.dto.WithdrawRequest;
import com.ezwallet.module.topup.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopupService — BVA, EP, Decision Table, Pairwise Tests")
class TopupServiceTest {

    @Mock WalletService walletService;
    @Mock WalletRepository walletRepository;
    @Mock UserRepository userRepository;
    @Mock BankAccountService bankAccountService;
    @Mock FeeCalculationService feeCalculationService;
    @Mock TransactionLimitService transactionLimitService;
    @Mock TransactionRepository transactionRepository;
    @Mock OtpService otpService;
    @Mock TransactionMapper transactionMapper;

    @InjectMocks TopupService topupService;

    private User user;
    private Wallet wallet;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setTier(UserTier.BRONZE);

        wallet = new Wallet();
        wallet.setId(10L);
        wallet.setBalance(new BigDecimal("100000"));
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUser(user);

        bankAccount = BankAccount.builder()
                .id(100L)
                .user(user)
                .bankCode("VCB")
                .accountNumber("1234567890")
                .accountHolder("Nguyen Van A")
                .build();

        // lenient: shared setup stubs that are skipped when idempotency short-circuits
        lenient().when(walletService.getWallet(1L)).thenReturn(wallet);
        lenient().when(bankAccountService.findOwned(eq(1L), anyLong())).thenReturn(bankAccount);
        lenient().when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionMapper.toDto(any())).thenReturn(
                new TransactionResponse(null, null, TransactionType.TOPUP, TransactionStatus.COMPLETED,
                        BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null));
    }

    // ─── deposit tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deposit — BVA on amount")
    class DepositBva {

        @Test
        @DisplayName("BVA-01: amount 10000 (min boundary per Vietnam Bank standard) → accepted")
        void depositMinBoundaryAccepted() {
            when(walletService.creditWallet(anyLong(), any())).thenReturn(wallet);

            TopupRequest req = new TopupRequest(100L, new BigDecimal("10000"), null);
            assertThatNoException().isThrownBy(() -> topupService.deposit(1L, req));
        }

        @Test
        @DisplayName("BVA-02: idempotency key already exists → return existing tx, no double credit")
        void depositIdempotencyKeyReturnsExisting() {
            Transaction existing = Transaction.builder()
                    .idempotencyKey("key-123")
                    .status(TransactionStatus.COMPLETED)
                    .build();
            when(transactionRepository.findByIdempotencyKey("key-123"))
                    .thenReturn(Optional.of(existing));

            TopupRequest req = new TopupRequest(100L, new BigDecimal("50000"), "key-123");
            topupService.deposit(1L, req);

            verify(walletService, never()).creditWallet(anyLong(), any());
        }

        @Test
        @DisplayName("BVA-03: new idempotency key → credit wallet once")
        void depositNewIdempotencyKey_credits() {
            when(transactionRepository.findByIdempotencyKey("new-key")).thenReturn(Optional.empty());
            when(walletService.creditWallet(anyLong(), any())).thenReturn(wallet);

            TopupRequest req = new TopupRequest(100L, new BigDecimal("50000"), "new-key");
            topupService.deposit(1L, req);

            verify(walletService, times(1)).creditWallet(eq(wallet.getId()), eq(new BigDecimal("50000")));
        }

        @Test
        @DisplayName("BVA-04: wallet credit fails → tx saved as FAILED")
        void depositCreditFails_txMarkedFailed() {
            when(walletService.creditWallet(anyLong(), any()))
                    .thenThrow(new BusinessException("WALLET_NOT_ACTIVE",
                            "Ví bị khoá", HttpStatus.UNPROCESSABLE_ENTITY));

            TopupRequest req = new TopupRequest(100L, new BigDecimal("50000"), null);
            assertThatThrownBy(() -> topupService.deposit(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("WALLET_NOT_ACTIVE");

            verify(transactionRepository, atLeastOnce()).save(argThat(tx ->
                    ((Transaction) tx).getStatus() == TransactionStatus.FAILED));
        }
    }

    // ─── withdraw tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("withdraw — Decision Table on fee (tier × type)")
    class WithdrawFeeDecisionTable {

        @BeforeEach
        void stubUser() {
            lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("DT-01: BRONZE tier → fee = 5000, debit = amount + 5000")
        void bronzeWithdrawFee5000() {
            user.setTier(UserTier.BRONZE);
            BigDecimal amount = new BigDecimal("200000");
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.WITHDRAW), anyString());
            when(feeCalculationService.calculate(TransactionType.WITHDRAW, UserTier.BRONZE, amount))
                    .thenReturn(new BigDecimal("5000"));
            when(walletService.debitWallet(anyLong(), any())).thenReturn(wallet);

            WithdrawRequest req = new WithdrawRequest(100L, amount, "123456", null);
            topupService.withdraw(1L, req);

            verify(walletService).debitWallet(eq(wallet.getId()), eq(new BigDecimal("205000")));
        }

        @Test
        @DisplayName("DT-02: SILVER tier → fee = 5000, debit = amount + 5000")
        void silverWithdrawFee5000() {
            user.setTier(UserTier.SILVER);
            BigDecimal amount = new BigDecimal("200000");
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.WITHDRAW), anyString());
            when(feeCalculationService.calculate(TransactionType.WITHDRAW, UserTier.SILVER, amount))
                    .thenReturn(new BigDecimal("5000"));
            when(walletService.debitWallet(anyLong(), any())).thenReturn(wallet);

            WithdrawRequest req = new WithdrawRequest(100L, amount, "123456", null);
            topupService.withdraw(1L, req);

            verify(walletService).debitWallet(eq(wallet.getId()), eq(new BigDecimal("205000")));
        }

        @Test
        @DisplayName("DT-03: GOLD tier → fee = 0, debit = amount only")
        void goldWithdrawFeeZero() {
            user.setTier(UserTier.GOLD);
            BigDecimal amount = new BigDecimal("1000000");
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.WITHDRAW), anyString());
            when(feeCalculationService.calculate(TransactionType.WITHDRAW, UserTier.GOLD, amount))
                    .thenReturn(BigDecimal.ZERO);
            when(walletService.debitWallet(anyLong(), any())).thenReturn(wallet);

            WithdrawRequest req = new WithdrawRequest(100L, amount, "123456", null);
            topupService.withdraw(1L, req);

            verify(walletService).debitWallet(eq(wallet.getId()), eq(new BigDecimal("1000000")));
        }

        @Test
        @DisplayName("DT-04: OTP invalid → BusinessException OTP_INVALID propagates")
        void invalidOtp_throwsException() {
            doThrow(new BusinessException("OTP_INVALID", "Mã OTP không đúng", HttpStatus.BAD_REQUEST))
                    .when(otpService).verify(any(), eq(OtpPurpose.WITHDRAW), eq("000000"));

            WithdrawRequest req = new WithdrawRequest(100L, new BigDecimal("100000"), "000000", null);
            assertThatThrownBy(() -> topupService.withdraw(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("OTP_INVALID");
        }
    }

    @Nested
    @DisplayName("withdraw — State Transition: wallet FROZEN → debit rejected")
    class WalletStateTransition {

        @Test
        @DisplayName("ST-01: wallet FROZEN → debitWallet throws WALLET_NOT_ACTIVE, tx saved FAILED")
        void frozenWallet_debitRejected() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.WITHDRAW), anyString());
            when(feeCalculationService.calculate(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(walletService.debitWallet(anyLong(), any()))
                    .thenThrow(new BusinessException("WALLET_NOT_ACTIVE",
                            "Ví không ở trạng thái hoạt động", HttpStatus.UNPROCESSABLE_ENTITY));

            WithdrawRequest req = new WithdrawRequest(100L, new BigDecimal("100000"), "123456", null);
            assertThatThrownBy(() -> topupService.withdraw(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("WALLET_NOT_ACTIVE");

            verify(transactionRepository, atLeastOnce()).save(argThat(tx ->
                    ((Transaction) tx).getStatus() == TransactionStatus.FAILED));
        }
    }

    @Nested
    @DisplayName("Pairwise: tier × amount range × bank isDefault")
    class PairwiseTests {

        /**
         * Pairwise covering tier (BRONZE/GOLD), amount range (small/large),
         * bank isDefault (true/false). 4 test cases cover all 2-way combinations.
         * P1: BRONZE, small=50k,   default=true  → fee=5000
         * P2: BRONZE, large=4M,    default=false → fee=5000
         * P3: GOLD,   small=50k,   default=false → fee=0
         * P4: GOLD,   large=4M,    default=true  → fee=0
         */
        @ParameterizedTest(name = "P{0}: tier={1} amount={2} default={3} expectedFee={4}")
        @CsvSource({
                "1, BRONZE, 50000,   true,  5000",
                "2, BRONZE, 4000000, false, 5000",
                "3, GOLD,   50000,   false, 0",
                "4, GOLD,   4000000, true,  0"
        })
        @DisplayName("Pairwise withdraw scenarios")
        void pairwiseWithdraw(int scenario, UserTier tier, BigDecimal amount,
                              boolean isDefault, BigDecimal expectedFee) {
            user.setTier(tier);
            bankAccount.setDefault(isDefault);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            doNothing().when(otpService).verify(any(), eq(OtpPurpose.WITHDRAW), anyString());
            when(feeCalculationService.calculate(eq(TransactionType.WITHDRAW), eq(tier), eq(amount)))
                    .thenReturn(expectedFee);
            when(walletService.debitWallet(anyLong(), any())).thenReturn(wallet);

            WithdrawRequest req = new WithdrawRequest(100L, amount, "123456", null);
            topupService.withdraw(1L, req);

            verify(walletService).debitWallet(eq(wallet.getId()), eq(amount.add(expectedFee)));
        }
    }
}
