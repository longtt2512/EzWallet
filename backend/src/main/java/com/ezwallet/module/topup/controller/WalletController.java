package com.ezwallet.module.topup.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.topup.dto.WalletResponse;
import com.ezwallet.module.topup.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wallet", description = "Thông tin ví")
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Lấy thông tin ví")
    @GetMapping("/wallet")
    public ApiResponse<WalletResponse> getWallet(Authentication auth) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        var wallet = walletService.getWallet(userId);
        return ApiResponse.ok(new WalletResponse(wallet.getId(), wallet.getBalance(),
                wallet.getCurrency(), wallet.getStatus()));
    }
}
