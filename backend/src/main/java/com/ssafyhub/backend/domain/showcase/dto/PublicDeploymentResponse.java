package com.ssafyhub.backend.domain.showcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공개 서비스 정보")
public record PublicDeploymentResponse(
        @Schema(description = "배포 ID", example = "1")
        Long deploymentId,

        @Schema(description = "팀명", example = "SSAFYHUB")
        String teamName,

        @Schema(description = "프로젝트 이름", example = "ssafyhub-showcase")
        String projectName,

        @Schema(description = "서비스 설명", example = "SSAFYHUB 쇼케이스 데모 서비스입니다.")
        String description,

        @Schema(description = "접속 URL", example = "https://1234567.ssafyhub.site")
        String endpointUrl,

        @Schema(description = "마지막 수정 시각", example = "2026-07-28T12:00:00")
        LocalDateTime updatedAt
) {

}
