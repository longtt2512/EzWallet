package com.ezwallet.module.auth.dto;

import com.ezwallet.module.account.entity.UserStatus;
import com.ezwallet.module.account.entity.UserTier;

public record UserProfileDto(
        Long id,
        String username,
        String email,
        String phone,
        String fullName,
        String avatarUrl,
        UserStatus status,
        UserTier tier
) {}
