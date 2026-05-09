package com.ezwallet.module.auth.service;

import com.ezwallet.common.Constants;
import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.account.entity.UserStatus;
import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.entity.OtpToken;
import com.ezwallet.module.auth.repository.OtpTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock OtpTokenRepository otpTokenRepository;
    @Mock JavaMailSender mailSender;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks OtpService otpService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .tier(UserTier.BRONZE)
                .build();
    }

    private OtpToken buildToken(int attempts, boolean used, Instant expiresAt) {
        return OtpToken.builder()
                .id(1L)
                .user(user)
                .contact("test@example.com")
                .purpose(OtpPurpose.REGISTER)
                .codeHash("$2a$12$hashedCode")
                .attempts(attempts)
                .expiresAt(expiresAt)
                .used(used)
                .build();
    }

    // =========================================================================
    // BVA — OTP TTL
    // =========================================================================

    @Test
    @DisplayName("BVA: OTP còn hiệu lực → verify thành công")
    void verify_validOtp_success() {
        OtpToken token = buildToken(0, false, Instant.now().plus(1, ChronoUnit.MINUTES));
        when(otpTokenRepository.findLatestValid(eq(1L), eq(OtpPurpose.REGISTER), any(Instant.class)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", token.getCodeHash())).thenReturn(true);

        assertThatCode(() -> otpService.verify(user, OtpPurpose.REGISTER, "123456"))
                .doesNotThrowAnyException();
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("BVA: OTP đã hết hạn → OTP_NOT_FOUND")
    void verify_expiredOtp_throws() {
        when(otpTokenRepository.findLatestValid(eq(1L), eq(OtpPurpose.REGISTER), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verify(user, OtpPurpose.REGISTER, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("OTP_NOT_FOUND");
    }

    // =========================================================================
    // BVA — OTP attempts: max = OTP_MAX_ATTEMPTS (5)
    // =========================================================================

    @Test
    @DisplayName("BVA: 4 lần sai (limit-1) → OTP_INVALID, token chưa bị invalidate")
    void verify_wrongCode_attempts4_notInvalidated() {
        OtpToken token = buildToken(3, false, Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpTokenRepository.findLatestValid(eq(1L), eq(OtpPurpose.REGISTER), any(Instant.class)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> otpService.verify(user, OtpPurpose.REGISTER, "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("OTP_INVALID");
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getAttempts()).isEqualTo(4);
    }

    @Test
    @DisplayName("BVA: 5 lần sai (đúng limit) → OTP_MAX_ATTEMPTS, token bị đánh dấu used")
    void verify_wrongCode_attempts5_tokenInvalidated() {
        OtpToken token = buildToken(4, false, Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpTokenRepository.findLatestValid(eq(1L), eq(OtpPurpose.REGISTER), any(Instant.class)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> otpService.verify(user, OtpPurpose.REGISTER, "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("OTP_MAX_ATTEMPTS");
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("Generate OTP → lưu token với expiresAt đúng TTL")
    void generate_savesTokenWithCorrectTtl() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(otpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        otpService.generate(user, "test@example.com", OtpPurpose.REGISTER);

        verify(otpTokenRepository).save(argThat(t -> {
            Instant exp = t.getExpiresAt();
            long ttl = Constants.OTP_TTL_SECONDS;
            return exp.isAfter(before.plusSeconds(ttl - 2))
                    && exp.isBefore(before.plusSeconds(ttl + 5));
        }));
    }
}
