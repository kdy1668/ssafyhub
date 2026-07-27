package com.ssafyhub.backend.domain.showcase.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PublicDeploymentListResponse(
        List<PublicDeploymentResponse> deployments,
        int page,
        int size,
        long totalElements
) {

    public static PublicDeploymentListResponse from(Page<PublicDeploymentResponse> page) {
        return new PublicDeploymentListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
