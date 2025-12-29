package com.studywithme.member.service;

import com.studywithme.global.exception.ApiException;
import com.studywithme.global.exception.ErrorCode;
import com.studywithme.global.security.jwt.AccessTokenBlacklist;
import com.studywithme.global.security.jwt.AccessTokenBlacklistRepository;
import com.studywithme.global.security.jwt.JwtTokenProvider;
import com.studywithme.global.security.jwt.TokenPair;
import com.studywithme.global.util.HashingUtil;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.domain.LoginAttempt;
import com.studywithme.member.dto.LoginRequest;
import com.studywithme.member.dto.MemberMeResponse;
import com.studywithme.member.dto.RefreshTokenRequest;
import com.studywithme.member.dto.SignupRequest;
import com.studywithme.member.dto.TokenResponse;
import com.studywithme.member.repository.LoginAttemptRepository;
import com.studywithme.member.repository.MemberRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOGIN_BLOCK_MINUTES = 10;

    private final MemberRepository memberRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            LoginAttemptRepository loginAttemptRepository,
            AccessTokenBlacklistRepository accessTokenBlacklistRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.accessTokenBlacklistRepository = accessTokenBlacklistRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public MemberMeResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        MemberRole role = resolveRole(request.role());
        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                role,
                MemberStatus.ACTIVE
        );
        Member saved = memberRepository.save(member);
        return toMeResponse(saved);
    }

    public TokenResponse login(LoginRequest request) {
        verifyLoginNotBlocked(request.email());
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    recordLoginFailure(request.email());
                    return new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            recordLoginFailure(request.email());
            throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        TokenPair tokenPair = jwtTokenProvider.issueTokens(member.getId(), member.getRole());
        member.updateRefreshToken(HashingUtil.sha256(tokenPair.refreshToken()), tokenPair.refreshTokenExpiresAt());
        clearLoginAttempt(request.email());
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        Long memberId = parseRefreshToken(request.refreshToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_UNAUTHORIZED));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN);
        }
        validateRefreshToken(member, request.refreshToken());

        TokenPair tokenPair = jwtTokenProvider.issueTokens(member.getId(), member.getRole());
        member.updateRefreshToken(HashingUtil.sha256(tokenPair.refreshToken()), tokenPair.refreshTokenExpiresAt());
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    public void logout(Long memberId, String accessToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
        member.clearRefreshToken();
        blacklistAccessToken(accessToken);
    }

    private void validateRefreshToken(Member member, String refreshToken) {
        if (member.getRefreshTokenHash() == null || member.getRefreshTokenExpiresAt() == null) {
            throw new ApiException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        if (member.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }
        String hashed = HashingUtil.sha256(refreshToken);
        if (!hashed.equals(member.getRefreshTokenHash())) {
            member.clearRefreshToken();
            throw new ApiException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }

    private Long parseRefreshToken(String refreshToken) {
        try {
            return jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (ExpiredJwtException ex) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException ex) {
            throw new ApiException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }

    private MemberRole resolveRole(MemberRole role) {
        if (role == null) {
            return MemberRole.PARTICIPANT;
        }
        if (role == MemberRole.ADMIN) {
            throw new ApiException(ErrorCode.COMMON_INVALID_REQUEST);
        }
        return role;
    }

    private MemberMeResponse toMeResponse(Member member) {
        return new MemberMeResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getStatus()
        );
    }

    private void verifyLoginNotBlocked(String email) {
        loginAttemptRepository.findById(email)
                .filter(attempt -> attempt.getBlockedUntil() != null)
                .filter(attempt -> attempt.getBlockedUntil().isAfter(LocalDateTime.now()))
                .ifPresent(attempt -> {
                    throw new ApiException(ErrorCode.AUTH_LOGIN_BLOCKED);
                });
    }

    private void recordLoginFailure(String email) {
        LoginAttempt attempt = loginAttemptRepository.findById(email)
                .orElseGet(() -> new LoginAttempt(email));
        attempt.recordFailure(MAX_LOGIN_FAIL_COUNT, LOGIN_BLOCK_MINUTES, LocalDateTime.now());
        loginAttemptRepository.save(attempt);
    }

    private void clearLoginAttempt(String email) {
        loginAttemptRepository.findById(email).ifPresent(attempt -> {
            attempt.reset();
            loginAttemptRepository.save(attempt);
        });
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parseClaims(accessToken);
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    claims.getExpiration().toInstant(),
                    ZoneId.systemDefault()
            );
            AccessTokenBlacklist entry = new AccessTokenBlacklist(HashingUtil.sha256(accessToken), expiresAt);
            accessTokenBlacklistRepository.save(entry);
        } catch (JwtException ex) {
            // ignore invalid tokens on logout
        }
    }
}
