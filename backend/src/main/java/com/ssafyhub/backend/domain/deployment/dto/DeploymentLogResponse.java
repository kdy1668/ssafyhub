package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "배포 진행 로그 한 줄 (깃헙 액션식 단계별 로그)")
public record DeploymentLogResponse(
        @Schema(description = "로그 메시지", example = "#5 transferring context: 52.38MB")
        String logMessage,

        @Schema(description = "그 시점의 배포 상태")
        DeploymentStatus status,

        @Schema(description = "기록 시각", example = "2026-07-30T02:38:10")
        LocalDateTime createdAt
) {
    public static DeploymentLogResponse from(DeploymentLog log) {
        return new DeploymentLogResponse(log.getLogMessage(), log.getStatus(), log.getCreatedAt());
    }
}
