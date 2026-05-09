package com.ezwallet.module.bill.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.bill.dto.*;
import com.ezwallet.module.bill.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bills", description = "Tra cứu và thanh toán hoá đơn")
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BillController {

    private final BillService billService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Danh sách nhà cung cấp dịch vụ")
    @GetMapping("/providers")
    public ApiResponse<List<BillProviderResponse>> listProviders() {
        return ApiResponse.ok(billService.listProviders());
    }

    @Operation(summary = "Tra cứu hoá đơn chưa thanh toán")
    @GetMapping("/lookup")
    public ApiResponse<BillResponse> lookup(@Valid BillLookupRequest req) {
        return ApiResponse.ok(billService.lookupBill(req));
    }

    @Operation(summary = "Gửi OTP xác thực thanh toán hoá đơn đến email")
    @PostMapping("/otp")
    public ApiResponse<Void> requestBillOtp(Authentication auth) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        billService.sendBillOtp(userId);
        return ApiResponse.ok(null, "Mã OTP đã được gửi đến email của bạn");
    }

    @Operation(summary = "Thanh toán hoá đơn (yêu cầu OTP)")
    @PostMapping("/pay")
    public ApiResponse<BillResponse> pay(Authentication auth,
                                          @Valid @RequestBody BillPayRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(billService.payBill(userId, req), "Thanh toán hoá đơn thành công");
    }
}
