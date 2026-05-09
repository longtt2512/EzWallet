package com.ezwallet.module.auth.dto;

import com.ezwallet.module.auth.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpResendRequest(
        @NotBlank String identifier,
        @NotNull OtpPurpose purpose
) {}
