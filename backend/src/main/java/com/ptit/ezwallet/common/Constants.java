package com.ptit.ezwallet.common;

import java.math.BigDecimal;

/**
 * Hằng số dùng chung toàn dự án.
 * Lưu ý: các giá trị nghiệp vụ (phí, hạn mức, ...) nên đẩy xuống database
 * (bảng FeeRule, TransactionLimit) để dễ kiểm thử bảng quyết định và biên.
 */
public final class Constants {

    private Constants() {}

    // ----- Tiền tệ -----
    public static final String DEFAULT_CURRENCY = "VND";
    public static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("1000");      // 1.000 VND
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("500000000"); // 500 triệu

    // ----- OTP -----
    public static final int OTP_LENGTH = 6;
    public static final int OTP_TTL_SECONDS = 300; // 5 phút
    public static final int OTP_MAX_ATTEMPTS = 5;

    // ----- Auth / khoá tài khoản -----
    public static final int LOGIN_MAX_FAILED_ATTEMPTS = 5;
    public static final int LOGIN_LOCK_DURATION_MINUTES = 15;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 64;

    // ----- API -----
    public static final String API_PREFIX = "/api/v1";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // ----- Header -----
    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    // ----- Redis prefix -----
    public static final String REDIS_PREFIX_OTP = "otp:";
    public static final String REDIS_PREFIX_LOGIN_ATTEMPT = "login:attempt:";
    public static final String REDIS_PREFIX_RATE_LIMIT = "rate:";
    public static final String REDIS_PREFIX_REFRESH_TOKEN = "rt:";
}
