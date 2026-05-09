package com.ezwallet.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã OTP gồm 6 chữ số") String otpCode,
        @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
