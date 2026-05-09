package com.ezwallet.module.auth.controller;

import com.ezwallet.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint công khai để smoke-test app đã chạy.
 * Truy cập: GET /api/v1/auth/ping
 */
@Tag(name = "Health", description = "Smoke-test endpoint")
@RestController
@RequestMapping("/auth")
public class HealthController {

    @Operation(summary = "Ping - kiểm tra app đã chạy")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "ezwallet-backend",
                "version", "0.0.1-SNAPSHOT"
        ));
    }
}
