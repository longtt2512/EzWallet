package com.ptit.ezwallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bind các cấu hình JWT từ application.yml (prefix: ezwallet.security.jwt).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ezwallet.security.jwt")
public class JwtProperties {

    /** Khoá bí mật ký HS256 - tối thiểu 32 ký tự */
    private String secret;

    /** Thời gian sống của access token (ms) */
    private long accessExpirationMs = 900_000L; // 15 phút

    /** Thời gian sống của refresh token (ms) */
    private long refreshExpirationMs = 604_800_000L; // 7 ngày

    /** Issuer */
    private String issuer = "ezwallet";
}
