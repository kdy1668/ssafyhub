package com.ssafyhub.backend.domain.artifact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "업로드용 Presigned URL 발급 응답")
public record PresignedUrlResponse(
        @Schema(description = "백엔드 산출물 업로드 정보")
        ArtifactUploadResponse backendArtifact,

        @Schema(description = "프론트엔드 산출물 업로드 정보")
        ArtifactUploadResponse frontendArtifact,

        @Schema(description = "Presigned URL 만료 시각", example = "2026-07-28T12:10:00")
        LocalDateTime expiresAt
) {
}
