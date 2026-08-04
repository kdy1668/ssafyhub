package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 배포된 서비스의 메타데이터(프로젝트 이름/설명/공개여부) 수정 요청.
 * null인 필드는 변경하지 않는다(부분 수정). 이름/설명은 실행 중인 컨테이너에 영향이 없어
 * 재배포 없이 즉시 반영된다. 공개 URL(slug=학번 기반)은 이름을 바꿔도 그대로다.
 */
@Schema(description = "배포 메타데이터 수정 요청. null인 필드는 변경하지 않습니다.")
public record DeploymentUpdateRequest(
        @Schema(description = "프로젝트 이름. null/공백이면 변경하지 않음", example = "우리팀 가계부")
        @Size(max = 100, message = "프로젝트 이름은 100자 이하여야 합니다.")
        String projectName,

        @Schema(description = "서비스 설명. null이면 변경하지 않음", example = "SSAFY 15기 공통 프로젝트")
        @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.")
        String description,

        @Schema(description = "공개 여부. null이면 변경하지 않음", example = "PUBLIC")
        Visibility visibility
) {
}
