package com.ssafyhub.backend.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요청 값 검증 실패 상세")
public record ValidationErrorResponse(
        @Schema(description = "검증에 실패한 필드명", example = "studentNumber")
        String field,

        @Schema(description = "검증 실패 사유", example = "학번은 필수입니다.")
        String message
) {
}
