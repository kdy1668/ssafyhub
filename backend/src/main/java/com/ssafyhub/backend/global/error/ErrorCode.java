package com.ssafyhub.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "MEMBER_001", "이미 사용 중인 학번입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_002", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMBER_003", "비밀번호는 최대 72바이트까지 입력할 수 있습니다."),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_001", "학번 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
