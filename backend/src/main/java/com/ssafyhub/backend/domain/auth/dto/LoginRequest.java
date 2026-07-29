package com.ssafyhub.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "학번", example = "1234567")
        @NotBlank(message = "학번은 필수입니다.")
        String studentNumber,

        @Schema(description = "비밀번호", example = "password123")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
