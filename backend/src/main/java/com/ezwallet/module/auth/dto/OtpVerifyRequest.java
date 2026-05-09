package com.ezwallet.module.auth.dto;

import com.ezwallet.module.auth.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank String identifier,
        @NotNull OtpPurpose purpose,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã OTP gồm 6 chữ số") String code
) {}
