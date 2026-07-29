package com.ssafyhub.backend.domain.artifact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "업로드용 Presigned URL 발급 요청")
public record PresignedUrlRequest(
        @Schema(description = "백엔드 산출물 파일 이름 (.jar)", example = "backend.jar")
        @NotBlank(message = "백엔드 파일 이름은 필수입니다.")
        @Size(max = 255, message = "백엔드 파일 이름은 255자를 초과할 수 없습니다.")
        String backendFileName,

        @Schema(description = "프론트엔드 산출물 파일 이름 (.zip)", example = "frontend.zip")
        @NotBlank(message = "프론트엔드 파일 이름은 필수입니다.")
        @Size(max = 255, message = "프론트엔드 파일 이름은 255자를 초과할 수 없습니다.")
        String frontendFileName
) {
}
