package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "배포 생성 응답. 배포는 비동기로 처리되므로 접수 시점의 상태만 담고 있습니다.")
public record DeploymentCreateResponse(
        @Schema(description = "생성된 배포 ID", example = "1")
        Long deploymentId,

        @Schema(description = "배포 상태")
        DeploymentStatus status,

        @Schema(description = "화면 표시용 상태 문구", example = "배포 대기중")
        String displayStatus,

        @Schema(description = "배포 완료 후 접속 예상 URL", example = "https://ssafyhub-showcase.ssafyhub.site")
        String expectedUrl,

        @Schema(description = "배포 요청 시각", example = "2026-07-28T12:00:00")
        LocalDateTime requestedAt
) {
}
