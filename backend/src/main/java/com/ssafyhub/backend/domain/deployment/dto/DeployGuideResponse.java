package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 배포 가이드 생성 응답.
 * - guide: 구조화된 분석 결과(프론트에서 항목별로 표시하거나 후속 자동화에 사용)
 * - markdown: 그대로 레포에 넣어 코딩 에이전트가 읽는 CLAUDE.md 형식 가이드
 */
@Schema(description = "배포 가이드 생성 응답")
public record DeployGuideResponse(
        DeployGuide guide,
        @Schema(description = "레포에 넣을 CLAUDE.md 형식 가이드(마크다운)")
        String markdown,
        @Schema(description = "사용한 모델", example = "gpt-4.1")
        String model
) {
}
