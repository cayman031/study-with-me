package com.studywithme.global.security;

import com.studywithme.global.exception.ApiException;
import com.studywithme.global.exception.ErrorCode;
import com.studywithme.global.security.jwt.MemberPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            throw new ApiException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return principal.memberId();
    }
}
