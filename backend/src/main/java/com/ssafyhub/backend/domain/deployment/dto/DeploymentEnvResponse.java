package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "환경변수 항목(복호화된 값)")
public record DeploymentEnvResponse(
        @Schema(description = "환경변수 이름", example = "SPRING_PROFILES_ACTIVE")
        String key,

        @Schema(description = "환경변수 값", example = "prod")
        String value
) {
}
