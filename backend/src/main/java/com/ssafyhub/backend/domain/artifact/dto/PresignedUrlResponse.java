package com.ssafyhub.backend.domain.artifact.dto;

import java.time.LocalDateTime;

public record PresignedUrlResponse(
        ArtifactUploadResponse backendArtifact,
        ArtifactUploadResponse frontendArtifact,
        LocalDateTime expiresAt
) {
}
