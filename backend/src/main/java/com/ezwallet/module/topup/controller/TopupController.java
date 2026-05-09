package com.ezwallet.module.topup.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.topup.dto.TopupRequest;
import com.ezwallet.module.topup.dto.TransactionResponse;
import com.ezwallet.module.topup.dto.WithdrawRequest;
import com.ezwallet.module.topup.service.TopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Top-up & Withdraw", description = "Nạp tiền và rút tiền")
@RestController
@RequestMapping("/topup")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TopupController {

    private final TopupService topupService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Gửi OTP xác thực nạp tiền đến email")
    @PostMapping("/deposit/otp")
    public ApiResponse<Void> requestDepositOtp(Authentication auth) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        topupService.sendDepositOtp(userId);
        return ApiResponse.ok(null, "Mã OTP đã được gửi đến email của bạn");
    }

    @Operation(summary = "Nạp tiền vào ví (yêu cầu OTP)")
    @PostMapping("/deposit")
    public ApiResponse<TransactionResponse> deposit(Authentication auth,
                                                     @Valid @RequestBody TopupRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(topupService.deposit(userId, req), "Nạp tiền thành công");
    }

    @Operation(summary = "Gửi OTP xác thực rút tiền đến email")
    @PostMapping("/withdraw/otp")
    public ApiResponse<Void> requestWithdrawOtp(Authentication auth) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        topupService.sendWithdrawOtp(userId);
        return ApiResponse.ok(null, "Mã OTP đã được gửi đến email của bạn");
    }

    @Operation(summary = "Rút tiền về ngân hàng (yêu cầu OTP)")
    @PostMapping("/withdraw")
    public ApiResponse<TransactionResponse> withdraw(Authentication auth,
                                                      @Valid @RequestBody WithdrawRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(topupService.withdraw(userId, req), "Rút tiền thành công");
    }
}
