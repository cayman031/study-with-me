package com.studywithme.member.dto;

import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;

public record MemberMeResponse(
        Long id,
        String email,
        String name,
        MemberRole role,
        MemberStatus status
) {
}
