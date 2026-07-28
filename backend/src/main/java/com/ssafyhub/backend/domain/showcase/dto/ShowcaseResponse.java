package com.ssafyhub.backend.domain.showcase.dto;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import java.time.LocalDateTime;

public record ShowcaseResponse(
        Long deploymentId,
        String projectName,
        String description,
        Visibility visibility,
        String endpointUrl,
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
