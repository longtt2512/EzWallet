package com.ezwallet.module.auth.service;

import com.ezwallet.common.Constants;
import com.ezwallet.config.JwtProperties;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.*;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.account.repository.WalletRepository;
import com.ezwallet.module.auth.dto.*;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.mapper.UserMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock WalletRepository walletRepository;
    @Mock OtpService otpService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;

    @InjectMocks AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .phone("0912345678")
                .passwordHash("$2a$12$hashedPassword")
                .status(UserStatus.ACTIVE)
                .tier(UserTier.BRONZE)
                .failedLoginAttempts(0)
                .build();
    }

    // =========================================================================
    // Register
    // =========================================================================

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký thành công")
        void register_success() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@ex.com")).thenReturn(false);
            when(userRepository.existsByPhone("0900000001")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(otpService.generate(any(), anyString(), any())).thenReturn("123456");

            var req = new RegisterRequest("newuser", "new@ex.com", "0900000001", "Password1!", "New User");
            assertThatCode(() -> authService.register(req)).doesNotThrowAnyException();
            verify(otpService).sendEmail(eq("new@ex.com"), anyString(), eq(OtpPurpose.REGISTER));
        }

        @Test
        @DisplayName("Đăng ký thất bại khi username đã tồn tại → CONFLICT")
        void register_duplicateUsername_throwsConflict() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);
            var req = new RegisterRequest("testuser", "x@x.com", "0900000002", "Password1!", null);
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("USERNAME_TAKEN");
        }
    }

    // =========================================================================
    // Decision table — Login
    // =========================================================================

    @Nested
    @DisplayName("Login — decision table")
    class LoginDecisionTable {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        }

        @Test
        @DisplayName("Rule 1: ACTIVE + mật khẩu đúng → token trả về")
        void login_active_correctPassword_returnsTokens() {
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("access");
            when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh");
            when(jwtProperties.getAccessExpirationMs()).thenReturn(900_000L);
            when(jwtProperties.getRefreshExpirationMs()).thenReturn(604_800_000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(userMapper.toDto(any())).thenReturn(new UserProfileDto(
                    1L, "testuser", "test@example.com", "0912345678", null, UserStatus.ACTIVE, UserTier.BRONZE));

            var result = authService.login(new LoginRequest("testuser", "correct"));
            assertThat(result.accessToken()).isEqualTo("access");
        }

        @Test
        @DisplayName("Rule 2: ACTIVE + mật khẩu sai → INVALID_CREDENTIALS 401")
        void login_active_wrongPassword_throws401() {
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "wrong")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Rule 3: PENDING_VERIFICATION → ACCOUNT_NOT_VERIFIED 403")
        void login_pending_throws403() {
            activeUser.setStatus(UserStatus.PENDING_VERIFICATION);

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "any")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("ACCOUNT_NOT_VERIFIED");
        }

        @Test
        @DisplayName("Rule 4: BANNED → ACCOUNT_BANNED 403")
        void login_banned_throws403() {
            activeUser.setStatus(UserStatus.BANNED);

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "any")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("ACCOUNT_BANNED");
        }

        @Test
        @DisplayName("Rule 5: LOCKED và lockedUntil chưa qua → ACCOUNT_LOCKED 403")
        void login_locked_lockNotExpired_throws403() {
            activeUser.setStatus(UserStatus.LOCKED);
            activeUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "any")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("ACCOUNT_LOCKED");
        }

        @Test
        @DisplayName("Rule 6: LOCKED và lockedUntil đã qua → tự mở khoá, đăng nhập được")
        void login_locked_lockExpired_autoUnlock() {
            activeUser.setStatus(UserStatus.LOCKED);
            activeUser.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("access");
            when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh");
            when(jwtProperties.getAccessExpirationMs()).thenReturn(900_000L);
            when(jwtProperties.getRefreshExpirationMs()).thenReturn(604_800_000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(userMapper.toDto(any())).thenReturn(new UserProfileDto(
                    1L, "testuser", "test@example.com", "0912345678", null, UserStatus.ACTIVE, UserTier.BRONZE));

            assertThatCode(() -> authService.login(new LoginRequest("testuser", "correct")))
                    .doesNotThrowAnyException();
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    // =========================================================================
    // BVA — failedLoginAttempts (limit = 5)
    // =========================================================================

    @Nested
    @DisplayName("Account lock — BVA trên failedLoginAttempts")
    class AccountLockBVA {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        }

        @Test
        @DisplayName("4 lần sai (limit-1=4) → chưa bị khoá, status vẫn ACTIVE")
        void fourFailedAttempts_notLocked() {
            activeUser.setFailedLoginAttempts(3);

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "bad")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(4);
        }

        @Test
        @DisplayName("5 lần sai (đúng limit=5) → tài khoản LOCKED")
        void fiveFailedAttempts_accountLocked() {
            activeUser.setFailedLoginAttempts(4);

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "bad")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.LOCKED);
            assertThat(activeUser.getLockedUntil()).isNotNull();
        }
    }

    // =========================================================================
    // State transition — UserStatus
    // =========================================================================

    @Nested
    @DisplayName("State transition — UserStatus")
    class UserStatusTransition {

        @Test
        @DisplayName("PENDING_VERIFICATION → ACTIVE sau khi verifyOtp(REGISTER)")
        void pendingToActive_afterOtpVerify() {
            activeUser.setStatus(UserStatus.PENDING_VERIFICATION);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
            doNothing().when(otpService).verify(any(), any(), anyString());

            authService.verifyOtp(new OtpVerifyRequest("testuser", OtpPurpose.REGISTER, "123456"));

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → LOCKED sau 5 lần sai mật khẩu")
        void activeToLocked_after5WrongPasswords() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            activeUser.setFailedLoginAttempts(4);

            assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "bad")))
                    .isInstanceOf(BusinessException.class);
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        }

        @Test
        @DisplayName("LOCKED → ACTIVE sau khi đặt lại mật khẩu thành công")
        void lockedToActive_afterResetPassword() {
            activeUser.setStatus(UserStatus.LOCKED);
            activeUser.setLockedUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
            activeUser.setFailedLoginAttempts(5);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
            doNothing().when(otpService).verify(any(), any(), anyString());
            when(passwordEncoder.encode(anyString())).thenReturn("newHash");
            when(redisTemplate.delete(anyString())).thenReturn(true);

            authService.resetPassword(new ResetPasswordRequest("testuser", "123456", "NewPassword1!"));

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(activeUser.getFailedLoginAttempts()).isZero();
            assertThat(activeUser.getLockedUntil()).isNull();
        }
    }
}
