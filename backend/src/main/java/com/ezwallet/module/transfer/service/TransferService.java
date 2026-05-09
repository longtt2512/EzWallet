package com.ezwallet.module.transfer.service;

import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.account.entity.Wallet;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.service.OtpService;
import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.repository.TransactionRepository;
import com.ezwallet.module.transaction.service.FeeCalculationService;
import com.ezwallet.module.transaction.service.TransactionLimitService;
import com.ezwallet.module.topup.service.WalletService;
import com.ezwallet.module.transfer.dto.P2PTransferRequest;
import com.ezwallet.module.transfer.dto.QrGenerateRequest;
import com.ezwallet.module.transfer.dto.QrPayRequest;
import com.ezwallet.module.transfer.dto.QrResponse;
import com.ezwallet.module.transfer.dto.TransferResponse;
import com.ezwallet.module.transfer.mapper.TransferMapper;
import com.ezwallet.util.IdempotencyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final FeeCalculationService feeCalculationService;
    private final TransactionLimitService transactionLimitService;
    private final TransactionRepository transactionRepository;
    private final TransferMapper transferMapper;
    private final QrCodeService qrCodeService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QR_KEY_PREFIX = "qr:";

    public void sendTransferOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        String code = otpService.generate(user, user.getEmail(), OtpPurpose.TRANSFER);
        otpService.sendEmail(user.getEmail(), code, OtpPurpose.TRANSFER);
    }

    @Transactional
    public TransferResponse p2pTransfer(Long senderId, P2PTransferRequest req) {
        if (req.idempotencyKey() != null) {
            var existing = transactionRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) return transferMapper.toDto(existing.get());
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        User receiver = resolveUser(req.receiverIdentifier());

        if (sender.getId().equals(receiver.getId())) {
            throw new BusinessException("SELF_TRANSFER",
                    "Không thể chuyển tiền cho chính mình", HttpStatus.BAD_REQUEST);
        }

        otpService.verify(sender, OtpPurpose.TRANSFER, req.otpCode());

        return executeTransfer(sender, receiver, req.amount(), req.note(),
                req.idempotencyKey());
    }

    public QrResponse generateQr(Long userId, QrGenerateRequest req) {
        String qrId = UUID.randomUUID().toString();
        int ttlMinutes = req.expiresInMinutes() != null ? req.expiresInMinutes() : 15;
        Instant expiresAt = Instant.now().plusSeconds((long) ttlMinutes * 60);

        Map<String, Object> qrMeta = new HashMap<>();
        qrMeta.put("receiverId", userId);
        qrMeta.put("amount", req.amount().toPlainString());
        qrMeta.put("note", req.note() != null ? req.note() : "");
        qrMeta.put("expiresAt", expiresAt.toString());
        qrMeta.put("paid", false);

        redisTemplate.opsForValue().set(QR_KEY_PREFIX + qrId, qrMeta,
                Duration.ofMinutes(ttlMinutes));

        String content = "ezwallet://pay?qrId=" + qrId + "&amount=" + req.amount().toPlainString();
        String imageUrl = qrCodeService.generateAndUpload(qrId, content);

        return new QrResponse(qrId, imageUrl, req.amount(), req.note(), expiresAt);
    }

    public QrResponse getQr(String qrId) {
        Map<?, ?> meta = getQrMeta(qrId);
        return new QrResponse(
                qrId,
                null,
                new BigDecimal((String) meta.get("amount")),
                (String) meta.get("note"),
                Instant.parse((String) meta.get("expiresAt"))
        );
    }

    @Transactional
    public TransferResponse payQr(Long payerId, QrPayRequest req) {
        Map<?, ?> meta = getQrMeta(req.qrId());

        if (Boolean.TRUE.equals(meta.get("paid"))) {
            throw new BusinessException("QR_ALREADY_PAID",
                    "Mã QR này đã được thanh toán", HttpStatus.CONFLICT);
        }

        Long receiverId = ((Number) meta.get("receiverId")).longValue();
        BigDecimal amount = new BigDecimal((String) meta.get("amount"));
        String note = (String) meta.get("note");

        // Mark QR as paid before executing to prevent concurrent double-pay
        Map<String, Object> updatedMeta = new HashMap<>();
        updatedMeta.put("receiverId", receiverId);
        updatedMeta.put("amount", amount.toPlainString());
        updatedMeta.put("note", note);
        updatedMeta.put("expiresAt", meta.get("expiresAt"));
        updatedMeta.put("paid", true);
        redisTemplate.opsForValue().set(QR_KEY_PREFIX + req.qrId(), updatedMeta,
                Duration.ofMinutes(5));

        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException("RECEIVER_NOT_FOUND",
                        "Không tìm thấy người nhận", HttpStatus.NOT_FOUND));

        if (payer.getId().equals(receiver.getId())) {
            throw new BusinessException("SELF_TRANSFER",
                    "Không thể thanh toán QR của chính mình", HttpStatus.BAD_REQUEST);
        }

        otpService.verify(payer, OtpPurpose.TRANSFER, req.otpCode());

        return executeTransfer(payer, receiver, amount, note, req.idempotencyKey());
    }

    @Transactional
    protected TransferResponse executeTransfer(User sender, User receiver,
                                               BigDecimal amount, String note,
                                               String idempotencyKey) {
        Wallet senderWallet = walletService.getWallet(sender.getId());
        Wallet receiverWallet = walletService.getWallet(receiver.getId());

        BigDecimal fee = feeCalculationService.calculate(
                TransactionType.TRANSFER, sender.getTier(), amount);
        BigDecimal total = amount.add(fee);

        transactionLimitService.checkLimit(senderWallet, sender.getTier(),
                TransactionType.TRANSFER, amount);

        String key = idempotencyKey != null
                ? idempotencyKey
                : IdempotencyUtil.generateIdempotencyKey(sender.getId(), TransactionType.TRANSFER, amount);

        BigDecimal senderBalanceBefore = senderWallet.getBalance();
        BigDecimal receiverBalanceBefore = receiverWallet.getBalance();

        Transaction tx = Transaction.builder()
                .transactionRef(IdempotencyUtil.generateTxRef())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .fee(fee)
                .sourceWallet(senderWallet)
                .targetWallet(receiverWallet)
                .idempotencyKey(key)
                .description(note != null && !note.isBlank() ? note
                        : "Chuyển tiền đến " + receiver.getUsername())
                .sourceBalanceBefore(senderBalanceBefore)
                .targetBalanceBefore(receiverBalanceBefore)
                .build();
        transactionRepository.save(tx);

        try {
            Wallet updatedSender = walletService.debitWallet(senderWallet.getId(), total);
            Wallet updatedReceiver = walletService.creditWallet(receiverWallet.getId(), amount);
            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setCompletedAt(Instant.now());
            tx.setSourceBalanceAfter(updatedSender.getBalance());
            tx.setTargetBalanceAfter(updatedReceiver.getBalance());
        } catch (Exception e) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(e.getMessage());
            transactionRepository.save(tx);
            throw e;
        }

        return transferMapper.toDto(transactionRepository.save(tx));
    }

    private User resolveUser(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new BusinessException("RECEIVER_NOT_FOUND",
                        "Không tìm thấy người nhận: " + identifier, HttpStatus.NOT_FOUND));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> getQrMeta(String qrId) {
        Object raw = redisTemplate.opsForValue().get(QR_KEY_PREFIX + qrId);
        if (raw == null) {
            throw new BusinessException("EXPIRED_QR",
                    "Mã QR đã hết hạn hoặc không tồn tại", HttpStatus.GONE);
        }
        return (Map<?, ?>) raw;
    }
}
