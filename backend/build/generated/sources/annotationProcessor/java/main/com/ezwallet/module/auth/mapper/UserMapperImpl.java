package com.ezwallet.module.auth.mapper;

import com.ezwallet.module.account.entity.User;
import com.ezwallet.module.account.entity.UserStatus;
import com.ezwallet.module.account.entity.UserTier;
import com.ezwallet.module.auth.dto.UserProfileDto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-09T23:48:30+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.7.jar, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserProfileDto toDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String username = null;
        String email = null;
        String phone = null;
        String fullName = null;
        String avatarUrl = null;
        UserStatus status = null;
        UserTier tier = null;

        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        phone = user.getPhone();
        fullName = user.getFullName();
        avatarUrl = user.getAvatarUrl();
        status = user.getStatus();
        tier = user.getTier();

        UserProfileDto userProfileDto = new UserProfileDto( id, username, email, phone, fullName, avatarUrl, status, tier );

        return userProfileDto;
    }
}
