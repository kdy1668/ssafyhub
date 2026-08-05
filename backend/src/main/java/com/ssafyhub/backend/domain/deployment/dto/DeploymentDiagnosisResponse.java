package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "배포 실패 AI 진단 결과")
public record DeploymentDiagnosisResponse(

        @Schema(description = "배포 id", example = "42")
        Long deploymentId,

        @Schema(description = "진단 대상이 된 실패 로그 id. 재배포로 다시 실패하면 값이 바뀐다.",
                example = "1573")
        Long sourceLogId,

        @Schema(description = "실패 원인 한 줄", example = "데이터베이스에 연결하지 못해 앱이 시작되지 못했습니다.")
        String summary,

        @Schema(description = "왜 그렇게 되었는지에 대한 설명")
        String detail,

        @Schema(description = "사용자가 화면에서 수행할 수 있는 해결 절차")
        List<FailureDiagnosis.Action> actions,

        @Schema(description = "근거가 된 로그 줄 번호. analyzedLog의 lineNumber와 같은 기준이다.",
                example = "[47, 48, 52]")
        List<Integer> logLineNumbers,

        @Schema(description = "진단 확신도. LOW면 참고용으로만 표시해야 한다.")
        FailureDiagnosis.Confidence confidence,

        @Schema(description = "진단에 사용한 로그 발췌(마스킹 후). 사용자가 근거를 직접 확인할 수 있도록 함께 반환한다.")
        List<DiagnosisLogLine> analyzedLog,

        @Schema(description = "줄 수 상한 때문에 앞에서 생략된 줄 수. 0이면 실패 사이클 전체가 포함되었다.",
                example = "0")
        int omittedLeadingLines,

        @Schema(description = "진단을 생성한 모델", example = "claude-opus-5")
        String model,

        @Schema(description = "true면 저장된 진단을 반환한 것이며 이번 요청에서 AI를 호출하지 않았다.",
                example = "false")
        boolean cached,

        @Schema(description = "진단 생성 시각")
        LocalDateTime createdAt
) {

    public static DeploymentDiagnosisResponse of(
            Long deploymentId,
            Long sourceLogId,
            FailureDiagnosis diagnosis,
            com.ssafyhub.backend.domain.deployment.support.FailureLogSlice slice,
            String model,
            boolean cached,
            LocalDateTime createdAt
    ) {
        return new DeploymentDiagnosisResponse(
                deploymentId,
                sourceLogId,
                diagnosis.summary(),
                diagnosis.detail(),
                diagnosis.actions() == null ? List.of() : diagnosis.actions(),
                diagnosis.logLineNumbers() == null ? List.of() : diagnosis.logLineNumbers(),
                diagnosis.confidence(),
                slice.lines(),
                slice.omittedLeadingLines(),
                model,
                cached,
                createdAt);
    }
}
