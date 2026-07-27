package com.ssafyhub.backend.domain.showcase.dto;

import com.ssafyhub.backend.domain.showcase.entity.ShowcaseDeployment;
import com.ssafyhub.backend.domain.showcase.enums.Visibility;
import java.time.LocalDateTime;

public record ShowcaseResponse(
        Long deploymentId,
        String projectName,
        String description,
        Visibility visibility,
        String endpointUrl,
        LocalDateTime updatedAt
) {

    public static ShowcaseResponse from(ShowcaseDeployment deployment) {
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
