package com.studywithme.global.exception;

import org.springframework.http.HttpStatus;

// docs/common/error-codes.md와 항상 동기화한다.
public enum ErrorCode {
    APPLICATION_ALREADY_CONFIRMED("APPLICATION_ALREADY_CONFIRMED", HttpStatus.OK, "이미 참가 확정된 신청입니다.", false),
    APPLICATION_ALREADY_CANCELED("APPLICATION_ALREADY_CANCELED", HttpStatus.OK, "이미 취소된 신청입니다.", false),
    APPLICATION_INVALID_TRANSITION("APPLICATION_INVALID_TRANSITION", HttpStatus.CONFLICT, "허용되지 않은 상태 전이입니다.", false),
    APPLICATION_DUPLICATED("APPLICATION_DUPLICATED", HttpStatus.CONFLICT, "이미 신청한 스터디입니다.", false),
    APPLICATION_NOT_FOUND("APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "신청 정보를 찾을 수 없습니다.", false),
    CAPACITY_INSUFFICIENT("CAPACITY_INSUFFICIENT", HttpStatus.CONFLICT, "스터디 정원이 부족합니다.", true),
    STUDY_CLOSED("STUDY_CLOSED", HttpStatus.CONFLICT, "모집이 종료된 스터디입니다.", false),
    STUDY_NOT_FOUND("STUDY_NOT_FOUND", HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다.", false),
    REVIEW_NOT_ALLOWED("REVIEW_NOT_ALLOWED", HttpStatus.FORBIDDEN, "리뷰 작성 권한이 없습니다.", false),
    REVIEW_DUPLICATED("REVIEW_DUPLICATED", HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다.", false),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.", false),
    AUTH_LOGIN_BLOCKED("AUTH_LOGIN_BLOCKED", HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 많아 잠시 후 다시 시도해 주세요.", true),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", true),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", true),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", false),
    MEMBER_EMAIL_DUPLICATED("MEMBER_EMAIL_DUPLICATED", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.", false),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.", false),
    ADMIN_ACTION_NOT_ALLOWED("ADMIN_ACTION_NOT_ALLOWED", HttpStatus.FORBIDDEN, "관리자 작업을 수행할 수 없습니다.", false),
    COMMON_INVALID_REQUEST("COMMON_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다.", false),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", true);

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
    private final boolean retryable;

    ErrorCode(String code, HttpStatus httpStatus, String message, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
