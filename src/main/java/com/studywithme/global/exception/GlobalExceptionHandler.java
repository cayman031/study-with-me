package com.studywithme.global.exception;

import com.studywithme.global.response.ErrorField;
import com.studywithme.global.response.ErrorResponse;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse body = ErrorResponse.of(errorCode, currentTraceId(), null);
        log.warn("apiException code={}, message={}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
        BindingResult bindingResult = extractBindingResult(ex);
        List<ErrorField> errors = toErrorFields(bindingResult);
        ErrorResponse body = ErrorResponse.of(ErrorCode.COMMON_INVALID_REQUEST, currentTraceId(), errors);
        log.warn("validationException errors={}", errors.size());
        return ResponseEntity.status(ErrorCode.COMMON_INVALID_REQUEST.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.AUTH_FORBIDDEN, currentTraceId(), null);
        log.warn("accessDenied message={}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.AUTH_FORBIDDEN.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.AUTH_UNAUTHORIZED, currentTraceId(), null);
        log.warn("authenticationException message={}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.AUTH_UNAUTHORIZED.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, currentTraceId(), null);
        log.error("unexpectedException", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(body);
    }

    private List<ErrorField> toErrorFields(BindingResult bindingResult) {
        return bindingResult.getFieldErrors()
                .stream()
                .map(this::toErrorField)
                .collect(Collectors.toList());
    }

    private ErrorField toErrorField(FieldError fieldError) {
        return new ErrorField(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                Objects.toString(fieldError.getDefaultMessage(), "")
        );
    }

    private BindingResult extractBindingResult(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manve) {
            return manve.getBindingResult();
        }
        if (ex instanceof BindException be) {
            return be.getBindingResult();
        }
        throw new IllegalArgumentException("Unsupported validation exception type");
    }

    private String currentTraceId() {
        return MDC.get("traceId");
    }
}
