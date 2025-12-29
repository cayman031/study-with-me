package com.studywithme.global.security.jwt;

import com.studywithme.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTtlMinutes = properties.getAccessTtlMinutes();
        this.refreshTtlDays = properties.getRefreshTtlDays();
    }

    public TokenPair issueTokens(Long memberId, MemberRole role) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(accessTtlMinutes * 60);
        Instant refreshExpiresAt = now.plusSeconds(refreshTtlDays * 24 * 60 * 60);

        String accessToken = Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiresAt))
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiresAt))
                .signWith(key)
                .compact();

        LocalDateTime refreshExpiresAtLocal = LocalDateTime.ofInstant(refreshExpiresAt, ZoneId.systemDefault());
        return new TokenPair(accessToken, refreshToken, refreshExpiresAtLocal);
    }

    public MemberPrincipal parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TYPE_ACCESS);
        Long memberId = Long.valueOf(claims.getSubject());
        MemberRole role = MemberRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new MemberPrincipal(memberId, role);
    }

    public Long parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TYPE_REFRESH);
        return Long.valueOf(claims.getSubject());
    }

    public Claims parseClaims(String token) throws ExpiredJwtException, JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateTokenType(Claims claims, String expected) {
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expected.equals(type)) {
            throw new JwtException("Invalid token type");
        }
    }
}
