package com.ezwallet.module.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
        @NotBlank @Size(min = 8, max = 64) String password,
        @Size(max = 150) String fullName
) {}
