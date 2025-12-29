package com.studywithme.global.security.jwt;

import com.studywithme.member.domain.MemberRole;

public record MemberPrincipal(Long memberId, MemberRole role) {
}
