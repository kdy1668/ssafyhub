package com.ssafyhub.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),

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
    CSRF_TOKEN_INVALID(HttpStatus.FORBIDDEN, "CSRF_001", "CSRF 토큰이 없거나 유효하지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_004", "회원을 찾을 수 없습니다."),

    INVALID_BACKEND_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_001", "백엔드 파일은 .jar 형식이어야 합니다."),
    INVALID_FRONTEND_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_002", "프론트엔드 파일은 .zip 형식이어야 합니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "ARTIFACT_003", "올바르지 않은 파일 이름입니다."),
    PRESIGNED_URL_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "ARTIFACT_004",
            "업로드 URL 발급에 실패했습니다."
    ),
    ARTIFACT_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTIFACT_005", "업로드 파일 정보를 찾을 수 없습니다."),
    ARTIFACT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ARTIFACT_006", "본인이 발급받은 파일만 사용할 수 있습니다."),
    ARTIFACT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "ARTIFACT_007", "업로드 파일 유형이 올바르지 않습니다."),
    ARTIFACT_ALREADY_USED(HttpStatus.CONFLICT, "ARTIFACT_008", "이미 사용된 업로드 파일입니다."),
    ARTIFACT_FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "ARTIFACT_009", "S3에 업로드된 파일이 존재하지 않습니다."),
    ARTIFACT_STORAGE_CHECK_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "ARTIFACT_010",
            "업로드 파일을 확인할 수 없습니다. 잠시 후 다시 시도해주세요."
    ),

    DEPLOYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "DEPLOYMENT_001", "사용자별 배포는 하나만 생성할 수 있습니다."),
    UNSUPPORTED_RUNTIME_VERSION(HttpStatus.BAD_REQUEST, "DEPLOYMENT_002", "지원하지 않는 Java 실행 버전입니다."),
    DUPLICATE_ENV_KEY(HttpStatus.BAD_REQUEST, "DEPLOYMENT_003", "중복된 환경변수 이름이 존재합니다."),
    DEPLOYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPLOYMENT_004", "조회할 배포 정보가 없습니다."),

    SHOWCASE_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOWCASE_001", "존재하지 않는 서비스입니다."),
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
