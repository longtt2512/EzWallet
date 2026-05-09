package com.ezwallet.module.auth.mapper;

import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.auth.dto.UserProfileDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileDto toDto(User user);
}
