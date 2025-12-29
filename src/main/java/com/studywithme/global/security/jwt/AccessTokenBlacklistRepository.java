package com.studywithme.global.security.jwt;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, String> {
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
}
