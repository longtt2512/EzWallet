package com.ezwallet.module.topup.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.topup.dto.BankAccountRequest;
import com.ezwallet.module.topup.dto.BankAccountResponse;
import com.ezwallet.module.topup.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bank Accounts", description = "Quản lý tài khoản ngân hàng liên kết")
@RestController
@RequestMapping("/account/bank-accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Danh sách tài khoản ngân hàng")
    @GetMapping
    public ApiResponse<List<BankAccountResponse>> list(Authentication auth) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(bankAccountService.listByUser(userId));
    }

    @Operation(summary = "Liên kết tài khoản ngân hàng mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BankAccountResponse> link(Authentication auth,
                                                   @Valid @RequestBody BankAccountRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(bankAccountService.link(userId, req), "Liên kết tài khoản thành công");
    }

    @Operation(summary = "Đặt làm tài khoản mặc định")
    @PatchMapping("/{id}/default")
    public ApiResponse<Void> setDefault(Authentication auth, @PathVariable Long id) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        bankAccountService.setDefault(userId, id);
        return ApiResponse.ok(null, "Đã đặt làm tài khoản mặc định");
    }

    @Operation(summary = "Xoá tài khoản ngân hàng")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        bankAccountService.delete(userId, id);
    }
}
