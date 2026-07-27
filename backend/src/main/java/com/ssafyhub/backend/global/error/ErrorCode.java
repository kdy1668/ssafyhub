package com.ssafyhub.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "MEMBER_001", "이미 사용 중인 학번입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_002", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMBER_003", "비밀번호는 최대 72바이트까지 입력할 수 있습니다."),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_001", "학번 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "Refresh Token이 존재하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_006", "만료된 Refresh Token입니다."),
    REVOKED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_007", "폐기된 Refresh Token입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_004", "회원을 찾을 수 없습니다."),

    INVALID_BACKEND_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_001", "백엔드 파일은 .jar 형식이어야 합니다."),
    INVALID_FRONTEND_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_002", "프론트엔드 파일은 .zip 형식이어야 합니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "ARTIFACT_003", "올바르지 않은 파일 이름입니다."),
    PRESIGNED_URL_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "ARTIFACT_004",
            "업로드 URL 발급에 실패했습니다."
    ),
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
