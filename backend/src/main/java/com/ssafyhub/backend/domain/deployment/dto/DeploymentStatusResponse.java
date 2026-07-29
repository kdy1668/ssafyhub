package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "내 배포 상태 조회 응답")
public record DeploymentStatusResponse(
        @Schema(description = "배포 요청 ID", example = "100")
        Long deploymentId,

        @Schema(description = "프로젝트 이름", example = "demo-service")
        String projectName,

        @Schema(description = "서비스 설명 (미입력 시 null)", example = "실시간 채팅 기반 협업 툴입니다.")
        String description,

        @Schema(description = "공개 여부", example = "PRIVATE")
        Visibility visibility,

        @Schema(description = "현재 배포 상태")
        DeploymentStatus status,

        @Schema(description = "화면 표시용 상태 문구", example = "정상 기동")
        String displayStatus,

        @Schema(description = "배포된 서비스 접속 URL", example = "https://1551343-demo-service-100.apps.ssafyhub.com")
        String endpointUrl,

        @Schema(description = "실패 사유 또는 상태 메시지", example = "null")
        String statusMessage,

        @Schema(description = "상태 마지막 변경 시각", example = "2026-07-16T16:25:00")
        LocalDateTime updatedAt
) {

    public static DeploymentStatusResponse of(Deployment deployment, DeploymentLog latestLog) {
        return new DeploymentStatusResponse(
                deployment.getId(),
                deployment.getProjectName(),
                deployment.getDescription(),
                deployment.getVisibility(),
                latestLog.getStatus(),
                latestLog.getStatus().displayStatus(),
                deployment.getEndpointUrl(),
                latestLog.getLogMessage(),
                latestLog.getCreatedAt()
        );
    }
}
