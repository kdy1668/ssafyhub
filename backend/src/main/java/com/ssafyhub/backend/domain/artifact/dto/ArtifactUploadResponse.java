package com.ssafyhub.backend.domain.artifact.dto;

public record ArtifactUploadResponse(
        Long artifactId,
        String uploadUrl
) {
}
