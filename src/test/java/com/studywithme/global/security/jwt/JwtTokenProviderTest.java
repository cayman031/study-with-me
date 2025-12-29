package com.studywithme.global.security.jwt;

import com.studywithme.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    @DisplayName("JWT 발급 및 파싱이 정상 동작한다")
    void issueAndParseTokens() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-32bytes-length!!!!--");
        properties.setAccessTtlMinutes(15);
        properties.setRefreshTtlDays(7);

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        TokenPair pair = provider.issueTokens(1L, MemberRole.LEADER);

        MemberPrincipal principal = provider.parseAccessToken(pair.accessToken());
        Long refreshMemberId = provider.parseRefreshToken(pair.refreshToken());

        assertThat(principal.memberId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo(MemberRole.LEADER);
        assertThat(refreshMemberId).isEqualTo(1L);
    }
}
