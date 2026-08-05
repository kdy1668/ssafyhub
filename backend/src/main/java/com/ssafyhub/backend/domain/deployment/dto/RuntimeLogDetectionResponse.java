package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "런타임 로그 이상 감지 결과")
public record RuntimeLogDetectionResponse(

        @Schema(description = "배포 id", example = "42")
        Long deploymentId,

        @Schema(description = "앱 상태 판정. UNKNOWN은 분석할 로그가 없었다는 뜻이며 AI를 호출하지 않았다.")
        DetectionHealth health,

        @Schema(description = "지금 앱이 어떤 상태인지",
                example = "앱은 정상 기동했지만, 최근 로그에서 같은 오류가 반복되고 있습니다.")
        String summary,

        @Schema(description = "발견한 이상 징후. 문제가 없으면 빈 배열이다.")
        List<RuntimeLogInsight.Finding> findings,

        @Schema(description = "권장 조치")
        List<String> nextSteps,

        @Schema(description = "분석한 로그 줄 수", example = "300")
        int analyzedLines,

        @Schema(description = "감지에 사용한 모델. AI를 호출하지 않았으면 null.", example = "claude-opus-5")
        String model,

        @Schema(description = "감지 시각")
        LocalDateTime detectedAt
) {

    /** 분석할 로그가 없어 AI를 호출하지 않은 경우. 오류가 아니라 정상 응답이다(FR-2.8). */
    public static RuntimeLogDetectionResponse unknown(Long deploymentId) {
        return new RuntimeLogDetectionResponse(
                deploymentId,
                DetectionHealth.UNKNOWN,
                "아직 분석할 로그가 없습니다. 앱이 출력한 로그가 쌓인 뒤에 다시 확인해 주세요.",
                List.of(),
                List.of(),
                0,
                null,
                LocalDateTime.now());
    }

    public static RuntimeLogDetectionResponse of(
            Long deploymentId, RuntimeLogInsight insight, int analyzedLines, String model) {
        return new RuntimeLogDetectionResponse(
                deploymentId,
                DetectionHealth.valueOf(insight.health().name()),
                insight.summary(),
                insight.findings() == null ? List.of() : insight.findings(),
                insight.nextSteps() == null ? List.of() : insight.nextSteps(),
                analyzedLines,
                model,
                LocalDateTime.now());
    }

    /**
     * 화면에 표시할 상태. LLM 스키마(RuntimeLogInsight.Health)에는 없는 UNKNOWN을 포함한다.
     * 두 enum을 분리한 이유는 RuntimeLogInsight 주석 참고.
     */
    public enum DetectionHealth {
        HEALTHY, WARNING, ERROR, UNKNOWN
    }
}
