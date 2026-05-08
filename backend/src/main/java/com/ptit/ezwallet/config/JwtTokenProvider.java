package com.ptit.ezwallet.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Tạo và xác thực JWT (HS256).
 * Tách thành 2 loại token: access (ngắn hạn) và refresh (dài hạn).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_USER_ID = "userId";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username) {
        return buildToken(userId, username, TYPE_ACCESS, jwtProperties.getAccessExpirationMs());
    }

    public String generateRefreshToken(Long userId, String username) {
        return buildToken(userId, username, TYPE_REFRESH, jwtProperties.getRefreshExpirationMs());
    }

    private String buildToken(Long userId, String username, String type, long ttlMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(exp)
                .claims(Map.of(CLAIM_USER_ID, userId, CLAIM_TOKEN_TYPE, type))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidAccessToken(String token) {
        return isValidToken(token, TYPE_ACCESS);
    }

    public boolean isValidRefreshToken(String token) {
        return isValidToken(token, TYPE_REFRESH);
    }

    private boolean isValidToken(String token, String expectedType) {
        try {
            Claims claims = parse(token);
            return expectedType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT [{}]: {}", expectedType, ex.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object userIdClaim = parse(token).get(CLAIM_USER_ID);
        if (userIdClaim instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
