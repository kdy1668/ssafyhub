package com.ssafyhub.backend.domain.showcase.dto;

import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서비스 수정 요청. null인 필드는 변경하지 않습니다.")
public record UpdateShowcaseRequest(
        @Schema(description = "공개 여부. null이면 변경하지 않음", example = "PUBLIC")
        Visibility visibility,

        @Schema(description = "서비스 설명. null이면 변경하지 않음", example = "SSAFYHUB 쇼케이스 데모 서비스입니다.")
        String description
) {

}
