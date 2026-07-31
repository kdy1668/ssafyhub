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

    INVALID_BACKEND_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_001", "백엔드 파일은 .jar 또는 Dockerfile을 포함한 .zip 형식이어야 합니다."),
    INVALID_FRONTEND_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_002", "프론트엔드 파일은 .zip 형식이어야 합니다."),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "ARTIFACT_003", "올바르지 않은 파일 이름입니다."),
    INVALID_SCHEMA_FILE(HttpStatus.BAD_REQUEST, "ARTIFACT_011", "DB 초기화 SQL 파일은 .sql 형식이어야 합니다."),
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
    DUPLICATE_ENV_KEY(HttpStatus.BAD_REQUEST, "DEPLOYMENT_003", "중복된 환경변수 이름이 존재합니다."),
    DEPLOYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPLOYMENT_004", "조회할 배포 정보가 없습니다."),
    DEPLOYMENT_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "DEPLOYMENT_005", "배포 리소스 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    DATABASE_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "DEPLOYMENT_006", "데이터베이스 이름을 입력했다면 비밀번호도 함께 입력해야 합니다."),
    UNSUPPORTED_NEEDED_SERVICE(HttpStatus.BAD_REQUEST, "DEPLOYMENT_007", "지원하지 않는 백킹서비스입니다. (지원: REDIS, MONGODB)"),
    UNSUPPORTED_DATABASE_ENGINE(HttpStatus.BAD_REQUEST, "DEPLOYMENT_008", "지원하지 않는 데이터베이스 엔진입니다. (지원: MYSQL)"),
    SCHEMA_REQUIRES_DATABASE(HttpStatus.BAD_REQUEST, "DEPLOYMENT_009", "DB 초기화 SQL 파일은 데이터베이스를 선택했을 때만 사용할 수 있습니다."),
    UNSUPPORTED_STACK(HttpStatus.BAD_REQUEST, "DEPLOYMENT_010",
            "지원하지 않는 언어/프레임워크입니다. (지원: SPRING_BOOT, DJANGO, FASTAPI, FLASK, EXPRESS)"),
    STACK_ARTIFACT_MISMATCH(HttpStatus.BAD_REQUEST, "DEPLOYMENT_011",
            "백엔드 아티팩트 형식이 선택한 스택과 맞지 않습니다. (Java 계열: .jar, Python/Node.js: .zip)"),
    MIGRATION_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "DEPLOYMENT_012",
            "데이터베이스를 사용한다면 테이블이 자동으로 생성되는지 여부를 알려주셔야 합니다."),
    SCHEMA_REQUIRED_WHEN_MANUAL_MIGRATION(HttpStatus.BAD_REQUEST, "DEPLOYMENT_013",
            "테이블이 자동으로 생성되지 않는다면 DB 초기화 SQL 파일이 필요합니다."),
    SCHEMA_NOT_ALLOWED_WHEN_AUTO_MIGRATION(HttpStatus.BAD_REQUEST, "DEPLOYMENT_020",
            "테이블이 자동으로 생성된다면 DB 초기화 SQL 파일을 사용할 수 없습니다."),
    NEED_AT_LEAST_ONE_ARTIFACT(HttpStatus.BAD_REQUEST, "DEPLOYMENT_014",
            "백엔드 또는 프론트엔드 중 최소 하나는 업로드해야 합니다."),
    RUNTIME_VERSION_REQUIRED(HttpStatus.BAD_REQUEST, "DEPLOYMENT_015", "백엔드를 올렸다면 런타임 버전은 필수입니다."),
    INTERNAL_PORT_REQUIRED(HttpStatus.BAD_REQUEST, "DEPLOYMENT_016",
            "백엔드를 올렸다면 내부 포트(1~65535)는 필수입니다."),
    DATABASE_REQUIRES_BACKEND(HttpStatus.BAD_REQUEST, "DEPLOYMENT_019",
            "데이터베이스는 백엔드가 있을 때만 사용할 수 있습니다."),

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
