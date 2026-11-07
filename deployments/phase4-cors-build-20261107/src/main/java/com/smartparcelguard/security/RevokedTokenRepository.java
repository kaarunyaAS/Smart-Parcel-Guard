package com.smartparcelguard.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenId(String tokenId);

    @Modifying
    @Query("DELETE FROM RevokedToken token WHERE token.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
