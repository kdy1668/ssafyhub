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
        LocalDateTime updatedAt,

        @Schema(description = "백엔드 아티팩트 보유 여부(재배포 시 백엔드 코드 교체 가능 여부)", example = "true")
        Boolean hasBackend,

        @Schema(description = "프론트엔드 아티팩트 보유 여부(재배포 시 프론트 교체 가능 여부)", example = "true")
        Boolean hasFrontend,

        @Schema(description = "스택(백엔드 재업로드 형식 판단용: SPRING_*는 .jar, 그 외는 .zip)", example = "DJANGO")
        String techStack
) {

    // accessUrl: 사용자에게 보여줄 접속 주소(서브도메인). deployment.endpoint_url(IP+경로)은 내부
    // 프록시 타깃이라 그대로 두고, 표시용으로는 {학번}.ssafyhub.site 를 내려준다.
    public static DeploymentStatusResponse of(Deployment deployment, DeploymentLog latestLog, String accessUrl) {
        return new DeploymentStatusResponse(
                deployment.getId(),
                deployment.getProjectName(),
                deployment.getDescription(),
                deployment.getVisibility(),
                latestLog.getStatus(),
                latestLog.getStatus().displayStatus(),
                accessUrl,
                latestLog.getLogMessage(),
                latestLog.getCreatedAt(),
                deployment.getBackendArtifact() != null,
                deployment.getFrontendArtifact() != null,
                deployment.getEffectiveTechStack()
        );
    }
}
