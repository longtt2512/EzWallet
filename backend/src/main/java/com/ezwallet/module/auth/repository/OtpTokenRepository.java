package com.ezwallet.module.auth.repository;

import com.ezwallet.module.auth.entity.OtpPurpose;
import com.ezwallet.module.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    @Query("""
            SELECT o FROM OtpToken o
            WHERE o.user.id = :userId
              AND o.purpose = :purpose
              AND o.used = false
              AND o.expiresAt > :now
            ORDER BY o.createdAt DESC
            LIMIT 1
            """)
    Optional<OtpToken> findLatestValid(
            @Param("userId") Long userId,
            @Param("purpose") OtpPurpose purpose,
            @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :before")
    void deleteExpiredBefore(@Param("before") Instant before);
}
