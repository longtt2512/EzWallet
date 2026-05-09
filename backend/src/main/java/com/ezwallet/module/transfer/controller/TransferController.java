package com.ezwallet.module.transfer.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.transfer.dto.*;
import com.ezwallet.module.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfer", description = "Chuyển tiền P2P và thanh toán QR")
@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Gửi OTP xác thực chuyển tiền đến email")
    @PostMapping("/otp")
    public ApiResponse<Void> requestTransferOtp(Authentication auth) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        transferService.sendTransferOtp(userId);
        return ApiResponse.ok(null, "Mã OTP đã được gửi đến email của bạn");
    }

    @Operation(summary = "Chuyển tiền P2P (yêu cầu OTP)")
    @PostMapping("/p2p")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransferResponse> p2p(Authentication auth,
                                              @Valid @RequestBody P2PTransferRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(transferService.p2pTransfer(userId, req), "Chuyển tiền thành công");
    }

    @Operation(summary = "Tạo mã QR nhận tiền")
    @PostMapping("/qr/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QrResponse> generateQr(Authentication auth,
                                               @Valid @RequestBody QrGenerateRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(transferService.generateQr(userId, req), "Tạo QR thành công");
    }

    @Operation(summary = "Lấy thông tin mã QR")
    @GetMapping("/qr/{qrId}")
    public ApiResponse<QrResponse> getQr(Authentication auth,
                                          @PathVariable String qrId) {
        return ApiResponse.ok(transferService.getQr(qrId));
    }

    @Operation(summary = "Thanh toán bằng mã QR (yêu cầu OTP)")
    @PostMapping("/qr/pay")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransferResponse> payQr(Authentication auth,
                                                @Valid @RequestBody QrPayRequest req) {
        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        return ApiResponse.ok(transferService.payQr(userId, req), "Thanh toán QR thành công");
    }
}
