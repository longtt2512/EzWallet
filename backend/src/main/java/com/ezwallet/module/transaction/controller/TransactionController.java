package com.ezwallet.module.transaction.controller;

import com.ezwallet.common.ApiResponse;
import com.ezwallet.common.PageResponse;
import com.ezwallet.config.JwtTokenProvider;
import com.ezwallet.module.transaction.dto.TransactionFilterRequest;
import com.ezwallet.module.transaction.entity.TransactionStatus;
import com.ezwallet.module.transaction.entity.TransactionType;
import com.ezwallet.module.transaction.service.TransactionService;
import com.ezwallet.module.topup.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Transactions", description = "Lịch sử giao dịch")
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Lịch sử giao dịch có lọc + phân trang")
    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> list(
            Authentication auth,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = jwtTokenProvider.extractUserId((String) auth.getCredentials());
        TransactionFilterRequest filter = new TransactionFilterRequest(
                type, status, dateFrom, dateTo, page, size);
        return ApiResponse.ok(transactionService.listTransactions(userId, filter));
    }
}
