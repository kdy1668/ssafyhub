package com.ssafyhub.backend.domain.showcase.dto;

import java.time.LocalDateTime;

public record PublicDeploymentResponse(
        Long deploymentId,
        String teamName,
        String projectName,
        String description,
        String endpointUrl,
        LocalDateTime updatedAt
) {

}
