package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "환경변수 항목")
public record DeploymentEnvRequest(
        @Schema(description = "환경변수 이름 (영문 대문자로 시작, 대문자/숫자/언더스코어)", example = "SPRING_PROFILES_ACTIVE")
        @NotBlank(message = "환경변수 이름은 필수입니다.")
        @Size(max = 100, message = "환경변수 이름은 100자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Z_][A-Z0-9_]*$",
                message = "환경변수 이름은 영문 대문자, 숫자, 언더스코어만 사용할 수 있고 숫자로 시작할 수 없습니다."
        )
        String key,

        @Schema(description = "환경변수 값", example = "prod")
        @NotBlank(message = "환경변수 값은 필수입니다.")
        @Size(max = 4000, message = "환경변수 값은 4000자를 초과할 수 없습니다.")
        String value
) {
}
