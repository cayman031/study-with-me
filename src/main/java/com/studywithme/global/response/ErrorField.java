package com.studywithme.global.response;

public record ErrorField(
        String field,
        Object value,
        String reason
) {
}
