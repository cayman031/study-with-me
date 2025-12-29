package com.studywithme.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.global.exception.ApiException;
import com.studywithme.global.exception.ErrorCode;
import com.studywithme.global.security.jwt.AccessTokenBlacklistRepository;
import com.studywithme.global.security.jwt.JwtTokenProvider;
import com.studywithme.global.security.jwt.TokenPair;
import com.studywithme.global.util.HashingUtil;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.dto.LoginRequest;
import com.studywithme.member.dto.RefreshTokenRequest;
import com.studywithme.member.repository.LoginAttemptRepository;
import com.studywithme.member.repository.MemberRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceSecurityTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Test
    @DisplayName("로그인 실패 5회 후 로그인 시도 시 차단된다")
    void loginBlockedAfterFailures() {
        Member member = new Member(
                "blocked@studywithme.com",
                passwordEncoder.encode("password123"),
                "blocked",
                MemberRole.PARTICIPANT,
                MemberStatus.ACTIVE
        );
        memberRepository.save(member);

        LoginRequest request = new LoginRequest("blocked@studywithme.com", "wrongpass");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS));
        }

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_LOGIN_BLOCKED));

        assertThat(loginAttemptRepository.findById("blocked@studywithme.com")).isPresent();
    }

    @Test
    @DisplayName("Refresh 토큰 재사용 감지 시 토큰이 무효화된다")
    void refreshReuseClearsToken() {
        Member member = new Member(
                "reuse@studywithme.com",
                passwordEncoder.encode("password123"),
                "reuse",
                MemberRole.PARTICIPANT,
                MemberStatus.ACTIVE
        );
        Member saved = memberRepository.save(member);
        TokenPair usedToken = jwtTokenProvider.issueTokens(saved.getId(), saved.getRole());
        member.updateRefreshToken(HashingUtil.sha256("different-refresh-token"), LocalDateTime.now().plusDays(7));

        RefreshTokenRequest request = new RefreshTokenRequest(usedToken.refreshToken());
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED));

        Member reloaded = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRefreshTokenHash()).isNull();
    }

    @Test
    @DisplayName("로그아웃 시 Access 토큰이 블랙리스트에 등록된다")
    void logoutAddsAccessTokenToBlacklist() {
        Member member = new Member(
                "logout@studywithme.com",
                passwordEncoder.encode("password123"),
                "logout",
                MemberRole.PARTICIPANT,
                MemberStatus.ACTIVE
        );
        Member saved = memberRepository.save(member);
        TokenPair pair = jwtTokenProvider.issueTokens(saved.getId(), saved.getRole());

        authService.logout(saved.getId(), pair.accessToken());

        assertThat(accessTokenBlacklistRepository.existsById(HashingUtil.sha256(pair.accessToken()))).isTrue();
    }
}
