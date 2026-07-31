package com.ssafyhub.backend.domain.artifact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "업로드용 Presigned URL 발급 요청")
public record PresignedUrlRequest(
        @Schema(description = "백엔드 산출물 파일 이름 (.jar 또는 Dockerfile 포함 .zip)", example = "backend.jar")
        @NotBlank(message = "백엔드 파일 이름은 필수입니다.")
        @Size(max = 255, message = "백엔드 파일 이름은 255자를 초과할 수 없습니다.")
        String backendFileName,

        @Schema(description = "프론트엔드 산출물 파일 이름 (.zip, 선택 - 백엔드가 프론트를 직접 서빙하는 " +
                "모놀리식 구조라면 비워두세요)", example = "frontend.zip", nullable = true)
        @Size(max = 255, message = "프론트엔드 파일 이름은 255자를 초과할 수 없습니다.")
        String frontendFileName,

        @Schema(description = "DB 초기화 SQL 파일 이름 (.sql, 선택 - MyBatis 등 자동 테이블 생성이 안 되는 경우에만)",
                example = "schema.sql", nullable = true)
        @Size(max = 255, message = "SQL 파일 이름은 255자를 초과할 수 없습니다.")
        String schemaFileName
) {
}
