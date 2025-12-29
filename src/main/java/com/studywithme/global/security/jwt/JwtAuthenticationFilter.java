package com.studywithme.global.security.jwt;

import com.studywithme.global.exception.ErrorCode;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.repository.MemberRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTH_ERROR_ATTRIBUTE = "authErrorCode";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            MemberRepository memberRepository,
            AccessTokenBlacklistRepository accessTokenBlacklistRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
        this.accessTokenBlacklistRepository = accessTokenBlacklistRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                if (isBlacklisted(token)) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_UNAUTHORIZED);
                    filterChain.doFilter(request, response);
                    return;
                }
                MemberPrincipal principal = jwtTokenProvider.parseAccessToken(token);
                Member member = memberRepository.findById(principal.memberId()).orElse(null);
                if (member == null) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_UNAUTHORIZED);
                } else if (member.getStatus() != MemberStatus.ACTIVE) {
                    throw new AccessDeniedException("member is not active");
                } else {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException ex) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_TOKEN_EXPIRED);
            } catch (JwtException ex) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_UNAUTHORIZED);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String token) {
        String hashed = com.studywithme.global.util.HashingUtil.sha256(token);
        return accessTokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(hashed, LocalDateTime.now());
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
