package com.ezwallet.module.auth.service;

import com.ezwallet.common.Constants;
import com.ezwallet.config.JwtProperties;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.config.MinioProperties;
import com.ezwallet.exception.BusinessException;
import com.ezwallet.module.account.entity.*;
import com.ezwallet.module.account.repository.UserRepository;
import com.ezwallet.module.account.repository.WalletRepository;
import com.ezwallet.module.auth.dto.*;
import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.mapper.UserMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException("USERNAME_TAKEN", "Tên đăng nhập đã được sử dụng", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("EMAIL_TAKEN", "Email đã được sử dụng", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new BusinessException("PHONE_TAKEN", "Số điện thoại đã được sử dụng", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .phone(req.phone())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .status(UserStatus.PENDING_VERIFICATION)
                .tier(UserTier.BRONZE)
                .failedLoginAttempts(0)
                .build();
        userRepository.save(user);

        // Wallet created but inactive until OTP verified
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(java.math.BigDecimal.ZERO)
                .currency(Constants.DEFAULT_CURRENCY)
                .status(WalletStatus.ACTIVE)
                .build();
        walletRepository.save(wallet);

        String code = otpService.generate(user, user.getEmail(), OtpPurpose.REGISTER);
        otpService.sendEmail(user.getEmail(), code, OtpPurpose.REGISTER);
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest req) {
        User user = findUserByIdentifier(req.identifier());
        otpService.verify(user, req.purpose(), req.code());

        if (req.purpose() == OtpPurpose.REGISTER && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
    }

    @Transactional
    public void resendOtp(OtpResendRequest req) {
        User user = findUserByIdentifier(req.identifier());
        String code = otpService.generate(user, user.getEmail(), req.purpose());
        otpService.sendEmail(user.getEmail(), code, req.purpose());
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = findUserByIdentifier(req.identifier());

        // Check lock
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Tài khoản bị khoá đến " + user.getLockedUntil(), HttpStatus.FORBIDDEN);
        }
        // Auto-unlock if lock expired
        if (user.getStatus() == UserStatus.LOCKED && (user.getLockedUntil() == null || user.getLockedUntil().isBefore(Instant.now()))) {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException("ACCOUNT_BANNED", "Tài khoản đã bị cấm", HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException("ACCOUNT_NOT_VERIFIED",
                    "Tài khoản chưa xác thực email, vui lòng kiểm tra hộp thư", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= Constants.LOGIN_MAX_FAILED_ATTEMPTS) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(Instant.now().plus(Constants.LOGIN_LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
                userRepository.save(user);
                throw new BusinessException("ACCOUNT_LOCKED",
                        "Sai mật khẩu quá " + Constants.LOGIN_MAX_FAILED_ATTEMPTS + " lần, tài khoản bị khoá "
                                + Constants.LOGIN_LOCK_DURATION_MINUTES + " phút",
                        HttpStatus.FORBIDDEN);
            }
            userRepository.save(user);
            throw new BusinessException("INVALID_CREDENTIALS",
                    "Sai mật khẩu, còn " + (Constants.LOGIN_MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts()) + " lần thử",
                    HttpStatus.UNAUTHORIZED);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        // Store refresh token in Redis
        String rtKey = Constants.REDIS_PREFIX_REFRESH_TOKEN + user.getId();
        redisTemplate.opsForValue().set(rtKey, refreshToken,
                jwtProperties.getRefreshExpirationMs(), TimeUnit.MILLISECONDS);

        return new LoginResponse(accessToken, refreshToken,
                jwtProperties.getAccessExpirationMs() / 1000, userMapper.toDto(user));
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest req) {
        if (!jwtTokenProvider.isValidRefreshToken(req.refreshToken())) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED);
        }
        Long userId = jwtTokenProvider.extractUserId(req.refreshToken());
        String rtKey = Constants.REDIS_PREFIX_REFRESH_TOKEN + userId;
        Object stored = redisTemplate.opsForValue().get(rtKey);
        if (stored == null || !stored.equals(req.refreshToken())) {
            throw new BusinessException("REFRESH_TOKEN_REVOKED", "Refresh token đã bị thu hồi", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        String newAccess = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String newRefresh = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());
        redisTemplate.opsForValue().set(rtKey, newRefresh,
                jwtProperties.getRefreshExpirationMs(), TimeUnit.MILLISECONDS);

        return new LoginResponse(newAccess, newRefresh,
                jwtProperties.getAccessExpirationMs() / 1000, userMapper.toDto(user));
    }

    public void logout(Long userId) {
        String rtKey = Constants.REDIS_PREFIX_REFRESH_TOKEN + userId;
        redisTemplate.delete(rtKey);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        // Revoke all sessions
        redisTemplate.delete(Constants.REDIS_PREFIX_REFRESH_TOKEN + userId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        User user = findUserByIdentifier(req.identifier());
        String code = otpService.generate(user, user.getEmail(), OtpPurpose.RESET_PASSWORD);
        otpService.sendEmail(user.getEmail(), code, OtpPurpose.RESET_PASSWORD);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = findUserByIdentifier(req.identifier());
        otpService.verify(user, OtpPurpose.RESET_PASSWORD, req.otpCode());
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        redisTemplate.delete(Constants.REDIS_PREFIX_REFRESH_TOKEN + user.getId());
    }

    @Transactional
    public UserProfileDto uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("INVALID_FILE", "File không hợp lệ", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("INVALID_FILE_TYPE", "Chỉ chấp nhận file ảnh (jpg, png, webp)", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException("FILE_TOO_LARGE", "Ảnh tối đa 5MB", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        String ext = resolveExtension(file.getOriginalFilename(), contentType);
        String objectName = "avatars/" + userId + "." + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketPublic())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new BusinessException("UPLOAD_FAILED", "Không thể tải ảnh lên: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Append cache-buster so browsers reload after each re-upload
        String avatarUrl = minioProperties.getEndpoint() + "/" + minioProperties.getBucketPublic()
                + "/" + objectName + "?v=" + System.currentTimeMillis();
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        if (req.phone() != null && !req.phone().isBlank() && !req.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(req.phone())) {
                throw new BusinessException("PHONE_TAKEN", "Số điện thoại đã được sử dụng", HttpStatus.CONFLICT);
            }
            user.setPhone(req.phone());
        }

        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName());
        }

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    // ---- helpers ----

    private String resolveExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (ext.matches("jpg|jpeg|png|webp|gif")) return ext.equals("jpeg") ? "jpg" : ext;
        }
        return switch (contentType) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "jpg";
        };
    }

    public User findUserByIdentifier(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Không tìm thấy tài khoản", HttpStatus.NOT_FOUND));
    }
}
