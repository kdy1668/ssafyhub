package com.ssafyhub.backend.global.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 기능 활성 여부")
public record AiAvailabilityResponse(
        @Schema(description = "AI 기능을 사용할 수 있는지 여부. false면 프론트는 AI 관련 UI를 렌더링하지 않는다.",
                example = "true")
        boolean enabled
) {
}
