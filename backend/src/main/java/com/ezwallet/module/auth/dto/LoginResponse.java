package com.ezwallet.module.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserProfileDto user
) {
    public LoginResponse(String accessToken, String refreshToken, long expiresIn, UserProfileDto user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
