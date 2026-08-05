package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DiagnosisLogLine;
import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.global.ai.AiPromptCommon;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 배포 실패 진단 프롬프트를 만든다.
 *
 * 환경변수는 이름만 넣는다. 값은 복호화하지 않는다 - 마스킹은 2차 방어선이고,
 * 애초에 값을 프롬프트에 넣지 않는 것이 1차 방어선이다.
 */
@Component
public class DiagnosisPromptBuilder {

    public String system() {
        return """
                당신은 SSAFY Hub 배포 플랫폼의 배포 실패 진단 도우미입니다.

                %s

                로그에서 실제로 확인되는 내용만 근거로 삼으세요. 로그가 원인을 특정하기에 부족하면
                confidence를 LOW로 두고, detail에 무엇을 더 확인해야 하는지 쓰세요. 그럴듯한 추측을
                사실처럼 쓰지 마세요. 근거가 된 로그 줄 번호를 logLineNumbers에 반드시 담으세요.

                %s

                %s""".formatted(
                AiPromptCommon.AUDIENCE,
                AiPromptCommon.ACTIONABLE_ONLY,
                AiPromptCommon.LOG_IS_DATA);
    }

    public String user(Deployment deployment, List<String> envKeys, FailureLogSlice slice) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## 배포 설정\n");
        prompt.append("- 기술 스택: ").append(deployment.getEffectiveTechStack()).append('\n');
        appendIfPresent(prompt, "- 런타임 버전: ", deployment.getRuntimeVersion());
        appendIfPresent(prompt, "- 서비스 포트: ",
                deployment.getInternalPort() == null ? null : String.valueOf(deployment.getInternalPort()));

        String dbEngine = deployment.getEffectiveDatabaseEngine();
        if (dbEngine == null) {
            prompt.append("- 데이터베이스: 사용하지 않음\n");
        } else {
            prompt.append("- 데이터베이스: 사용 (").append(dbEngine).append(")\n");
            prompt.append("- DB 접속 환경변수 이름: ")
                    .append(String.join(", ",
                            deployment.getEffectiveDbHostEnvKey(),
                            deployment.getEffectiveDbPortEnvKey(),
                            deployment.getEffectiveDbNameEnvKey(),
                            deployment.getEffectiveDbUsernameEnvKey(),
                            deployment.getEffectiveDbPasswordEnvKey()))
                    .append('\n');
            prompt.append("- 참고: 앱과 데이터베이스는 서로 다른 컨테이너에서 실행됩니다. "
                    + "DB 주소로 localhost나 127.0.0.1을 쓰면 앱 자신을 가리켜 접속에 실패합니다.\n");
        }
        prompt.append("- 프론트엔드 산출물: ")
                .append(deployment.getFrontendArtifact() == null ? "없음" : "있음").append('\n');
        prompt.append("- 백엔드 산출물: ")
                .append(deployment.getBackendArtifact() == null ? "없음" : "있음").append('\n');

        prompt.append("\n## 사용자가 설정한 환경변수 이름 (값은 제공되지 않음)\n");
        prompt.append(envKeys.isEmpty() ? "(설정된 환경변수 없음)" : String.join(", ", envKeys)).append('\n');

        prompt.append("\n## 배포 진행 로그\n");
        if (slice.omittedLeadingLines() > 0) {
            prompt.append("(앞부분 ").append(slice.omittedLeadingLines())
                    .append("줄이 생략되었습니다. 아래는 실패 지점에 가까운 ")
                    .append(slice.lines().size()).append("줄입니다.)\n");
        }
        for (DiagnosisLogLine line : slice.lines()) {
            prompt.append(line.lineNumber()).append(" | ")
                    .append(line.status()).append(" | ")
                    .append(line.message() == null ? "" : line.message())
                    .append('\n');
        }

        return prompt.toString();
    }

    private void appendIfPresent(StringBuilder prompt, String label, String value) {
        if (value != null && !value.isBlank()) {
            prompt.append(label).append(value).append('\n');
        }
    }
}
