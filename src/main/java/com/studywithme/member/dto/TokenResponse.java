package com.studywithme.member.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
