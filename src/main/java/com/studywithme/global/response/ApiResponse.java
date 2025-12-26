package com.studywithme.global.response;

import java.util.List;
import org.slf4j.MDC;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<ErrorField> errors,
        String traceId
) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, "SUCCESS", "요청이 성공했습니다.", data, null, traceId);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, MDC.get("traceId"));
    }
}
