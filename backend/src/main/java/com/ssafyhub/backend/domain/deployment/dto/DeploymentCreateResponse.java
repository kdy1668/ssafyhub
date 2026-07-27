package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import java.time.LocalDateTime;

public record DeploymentCreateResponse(
        Long deploymentId,
        DeploymentStatus status,
        String displayStatus,
        String expectedUrl,
        LocalDateTime requestedAt
) {
}
