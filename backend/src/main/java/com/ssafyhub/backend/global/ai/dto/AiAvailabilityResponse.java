package com.ssafyhub.backend.global.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 기능 활성 여부와 클라이언트가 필요한 설정값")
public record AiAvailabilityResponse(

        @Schema(description = "AI 기능을 사용할 수 있는지 여부. false면 프론트는 AI 관련 UI를 렌더링하지 않는다.",
                example = "true")
        boolean enabled,

        @Schema(description = "런타임 로그 이상 감지의 배포당 최소 호출 간격(초). "
                + "프론트는 이 값으로 버튼 쿨다운 카운트다운을 표시한다. "
                + "에러 응답에는 데이터를 담을 수 없어 여기서 함께 내려준다.",
                example = "30")
        int runtimeLogCooldownSeconds
) {
}
