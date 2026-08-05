package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "진단에 사용된 로그 한 줄 (비밀값 마스킹 후)")
public record DiagnosisLogLine(

        @Schema(description = "이 발췌 안에서의 줄 번호(1부터). logLineNumbers가 가리키는 번호와 같은 기준이다.",
                example = "47")
        int lineNumber,

        @Schema(description = "그 시점의 배포 상태")
        DeploymentStatus status,

        @Schema(description = "로그 메시지", example = "  Caused by: com.mysql.cj.jdbc.exceptions.CommunicationsException")
        String message
) {
}
