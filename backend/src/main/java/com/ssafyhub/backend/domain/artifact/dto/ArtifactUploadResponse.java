package com.ssafyhub.backend.domain.artifact.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개별 파일의 업로드 정보")
public record ArtifactUploadResponse(
        @Schema(description = "발급된 아티팩트 ID", example = "1")
        Long artifactId,

        @Schema(description = "S3 업로드용 Presigned URL. 이 URL로 파일을 직접 PUT 업로드합니다.")
        String uploadUrl
) {
}
