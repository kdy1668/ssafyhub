package com.ssafyhub.backend.domain.showcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "공개 서비스 목록 응답")
public record PublicDeploymentListResponse(
        @Schema(description = "공개 서비스 목록")
        List<PublicDeploymentResponse> deployments,

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "42")
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
