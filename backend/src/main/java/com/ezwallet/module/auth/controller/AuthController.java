package com.ezwallet.module.auth.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.auth.dto.*;
import com.ezwallet.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Đăng ký, đăng nhập, OTP, đổi mật khẩu")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Đăng ký tài khoản mới")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ApiResponse.ok(null, "Đăng ký thành công, vui lòng kiểm tra email để xác thực OTP");
    }

    @Operation(summary = "Xác thực OTP")
    @PostMapping("/otp/verify")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        authService.verifyOtp(req);
        return ApiResponse.ok(null, "Xác thực thành công");
    }

    @Operation(summary = "Gửi lại OTP")
    @PostMapping("/otp/resend")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody OtpResendRequest req) {
        authService.resendOtp(req);
        return ApiResponse.ok(null, "OTP mới đã được gửi đến email của bạn");
    }

    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req), "Đăng nhập thành công");
    }

    @Operation(summary = "Làm mới access token")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ApiResponse.ok(authService.refresh(req), "Token đã được làm mới");
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserDetails principal) {
        Long userId = jwtTokenProvider.extractUserId(
                ((org.springframework.security.core.Authentication)
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                        .getCredentials().toString());
        authService.logout(userId);
        return ApiResponse.ok(null, "Đăng xuất thành công");
    }

    @Operation(summary = "Đổi mật khẩu")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        Long userId = resolveUserId();
        authService.changePassword(userId, req);
        return ApiResponse.ok(null, "Đổi mật khẩu thành công");
    }

    @Operation(summary = "Quên mật khẩu - gửi OTP")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ApiResponse.ok(null, "OTP đặt lại mật khẩu đã được gửi");
    }

    @Operation(summary = "Đặt lại mật khẩu bằng OTP")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ApiResponse.ok(null, "Đặt lại mật khẩu thành công");
    }

    private Long resolveUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            return jwtTokenProvider.extractUserId(token);
        }
        throw new com.ezwallet.exception.BusinessException("UNAUTHORIZED", "Không xác định được người dùng",
                org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
}
