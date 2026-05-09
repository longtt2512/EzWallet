package com.ezwallet.module.auth.service;

import com.ezwallet.common.Constants;
import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.entity.OtpToken;
import com.ezwallet.module.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public String generate(User user, String contact, OtpPurpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpToken token = OtpToken.builder()
                .user(user)
                .contact(contact)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(Constants.OTP_TTL_SECONDS, ChronoUnit.SECONDS))
                .build();
        otpTokenRepository.save(token);
        return code;
    }

    @Transactional
    public void verify(User user, OtpPurpose purpose, String code) {
        OtpToken token = otpTokenRepository
                .findLatestValid(user.getId(), purpose, Instant.now())
                .orElseThrow(() -> new BusinessException("OTP_NOT_FOUND",
                        "Mã OTP không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST));

        token.setAttempts(token.getAttempts() + 1);

        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            if (token.getAttempts() >= Constants.OTP_MAX_ATTEMPTS) {
                token.setUsed(true);
                otpTokenRepository.save(token);
                throw new BusinessException("OTP_MAX_ATTEMPTS",
                        "Đã nhập sai OTP quá " + Constants.OTP_MAX_ATTEMPTS + " lần, vui lòng yêu cầu mã mới",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            otpTokenRepository.save(token);
            throw new BusinessException("OTP_INVALID",
                    "Mã OTP không đúng, còn " + (Constants.OTP_MAX_ATTEMPTS - token.getAttempts()) + " lần thử",
                    HttpStatus.BAD_REQUEST);
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    @Async
    public void sendEmail(String to, String code, OtpPurpose purpose) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("noreply@ezwallet.local");
            msg.setTo(to);
            msg.setSubject("[EzWallet] Mã xác thực OTP");
            msg.setText("Mã OTP của bạn cho thao tác " + purpose.name() + " là: " + code
                    + "\nMã có hiệu lực trong " + (Constants.OTP_TTL_SECONDS / 60) + " phút."
                    + "\nKhông chia sẻ mã này với bất kỳ ai.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Không thể gửi email OTP tới {}: {}", to, e.getMessage());
        }
    }
}
