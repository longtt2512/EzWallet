package com.ezwallet.module.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @Pattern(regexp = "^(\\+84|0)[3-9]\\d{8}$", message = "Số điện thoại không hợp lệ")
        String phone
) {}
