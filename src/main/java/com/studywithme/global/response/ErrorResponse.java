package com.studywithme.global.response;

import com.studywithme.global.exception.ErrorCode;
import java.util.List;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Object data,
        List<ErrorField> errors,
        String traceId
) {
    public static ErrorResponse of(ErrorCode errorCode, String traceId, List<ErrorField> errors) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), null, errors, traceId);
    }
}
