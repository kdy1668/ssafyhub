package com.ssafyhub.backend.domain.showcase.dto;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "서비스 수정 결과 응답")
public record ShowcaseResponse(
        @Schema(description = "배포 ID", example = "1")
        Long deploymentId,

        @Schema(description = "프로젝트 이름", example = "ssafyhub-showcase")
        String projectName,

        @Schema(description = "서비스 설명", example = "SSAFYHUB 쇼케이스 데모 서비스입니다.")
        String description,

        @Schema(description = "공개 여부")
        Visibility visibility,

        @Schema(description = "접속 URL", example = "https://1234567.ssafyhub.site")
        String endpointUrl,

        @Schema(description = "마지막 수정 시각", example = "2026-07-28T12:00:00")
        LocalDateTime updatedAt
) {

    public static ShowcaseResponse from(Deployment deployment) {
        return new ShowcaseResponse(
                deployment.getId(),
                deployment.getProjectName(),
                deployment.getDescription(),
                deployment.getVisibility(),
                deployment.getEndpointUrl(),
                deployment.getUpdatedAt()
        );
    }
}
