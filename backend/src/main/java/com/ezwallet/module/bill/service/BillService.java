package com.ezwallet.module.bill.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.bill.dto.BillLookupRequest;
import com.ezwallet.module.bill.dto.BillPayRequest;
import com.ezwallet.module.bill.dto.BillProviderResponse;
import com.ezwallet.module.bill.dto.BillResponse;
import com.ezwallet.module.bill.entity.Bill;
import com.ezwallet.module.bill.entity.BillProvider;
import com.ezwallet.module.bill.entity.BillStatus;
import com.ezwallet.module.bill.mapper.BillMapper;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.service.OtpService;
import com.ezwallet.module.bill.repository.BillProviderRepository;
import com.ezwallet.module.bill.repository.BillRepository;
import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import com.ezwallet.module.topup.service.WalletService;
import com.ezwallet.util.IdempotencyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillProviderRepository billProviderRepository;
    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final OtpService otpService;
    private final TransactionRepository transactionRepository;
    private final TransactionLimitService transactionLimitService;
    private final BillMapper billMapper;

    public void sendBillOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        String code = otpService.generate(user, user.getEmail(), OtpPurpose.BILL_PAYMENT);
        otpService.sendEmail(user.getEmail(), code, OtpPurpose.BILL_PAYMENT);
    }

    public List<BillProviderResponse> listProviders() {
        return billProviderRepository.findByActiveTrue().stream()
                .map(billMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillResponse lookupBill(BillLookupRequest req) {
        BillProvider provider = billProviderRepository.findByCode(req.providerCode())
                .orElseThrow(() -> new BusinessException("PROVIDER_NOT_FOUND",
                        "Không tìm thấy nhà cung cấp dịch vụ", HttpStatus.NOT_FOUND));

        Bill bill = billRepository.findByProviderIdAndCustomerCodeAndStatus(
                provider.getId(), req.customerCode(), BillStatus.UNPAID)
                .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND",
                        "Không tìm thấy hoá đơn chưa thanh toán", HttpStatus.NOT_FOUND));

        return billMapper.toDto(bill);
    }

    @Transactional
    public BillResponse payBill(Long userId, BillPayRequest req) {
        // Idempotency check
        if (req.idempotencyKey() != null) {
            var existing = transactionRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) {
                Bill bill = billRepository.findByBillCode(req.billCode())
                        .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND",
                                "Không tìm thấy hoá đơn", HttpStatus.NOT_FOUND));
                return billMapper.toDto(bill);
            }
        }

        Bill bill = billRepository.findByBillCode(req.billCode())
                .orElseThrow(() -> new BusinessException("BILL_NOT_FOUND",
                        "Không tìm thấy hoá đơn", HttpStatus.NOT_FOUND));

        if (bill.getStatus() != BillStatus.UNPAID) {
            throw new BusinessException("BILL_NOT_PAYABLE",
                    "Hoá đơn không ở trạng thái có thể thanh toán (trạng thái: " + bill.getStatus() + ")",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        otpService.verify(user, OtpPurpose.BILL_PAYMENT, req.otpCode());

        var wallet = walletService.getWallet(userId);
        transactionLimitService.checkLimit(wallet, user.getTier(),
                TransactionType.BILL_PAYMENT, bill.getAmount());

        String idempotencyKey = req.idempotencyKey() != null
                ? req.idempotencyKey()
                : IdempotencyUtil.generateIdempotencyKey(userId, TransactionType.BILL_PAYMENT, bill.getAmount());

        var balanceBefore = wallet.getBalance();

        Transaction tx = Transaction.builder()
                .transactionRef(IdempotencyUtil.generateTxRef())
                .type(TransactionType.BILL_PAYMENT)
                .status(TransactionStatus.PENDING)
                .amount(bill.getAmount())
                .sourceWallet(wallet)
                .idempotencyKey(idempotencyKey)
                .description("Thanh toán " + bill.getProvider().getName()
                        + " - KH " + bill.getCustomerCode())
                .sourceBalanceBefore(balanceBefore)
                .build();
        transactionRepository.save(tx);

        try {
            var updated = walletService.debitWallet(wallet.getId(), bill.getAmount());
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setCompletedAt(Instant.now());
            tx.setSourceBalanceAfter(updated.getBalance());
            transactionRepository.save(tx);

            bill.setStatus(BillStatus.PAID);
            bill.setPaidAt(Instant.now());
            bill.setPaidByUser(user);
            bill.setPaidByTx(tx);
            billRepository.save(bill);
        } catch (Exception e) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(e.getMessage());
            transactionRepository.save(tx);
            throw e;
        }

        return billMapper.toDto(bill);
    }
}
