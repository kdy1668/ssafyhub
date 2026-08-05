package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.global.ai.AiPromptCommon;
import org.springframework.stereotype.Component;

/**
 * 런타임 로그 이상 감지 프롬프트를 만든다.
 */
@Component
public class RuntimeLogPromptBuilder {

    public String system() {
        return """
                당신은 실행 중인 애플리케이션의 로그를 읽고 이상 징후를 찾아내는 도우미입니다.
                사용자가 알고 싶은 것은 "지금 내 서비스가 괜찮은 상태인가"입니다.

                %s

                전달받은 로그는 최근 일부 구간의 스냅샷입니다. 전체 이력이 아니므로,
                보이지 않는 구간에 대해 단정하지 마세요. 로그에 반복해서 나타나는 패턴과
                명확한 오류에 집중하세요.

                정상적으로 동작하는 것처럼 보이면 health를 HEALTHY로 두고 findings를 비우세요.
                문제를 만들어내지 마세요. 프레임워크가 기동 중 출력하는 일반적인 경고나
                INFO 로그를 이상 징후로 보고하지 마세요.

                findings의 각 항목에는 그 판단의 근거를 detail에 쓰세요. 몇 번 나타났는지,
                어떤 시점에 몰려 있는지처럼 로그에서 확인 가능한 사실을 쓰세요.

                %s

                %s""".formatted(
                AiPromptCommon.AUDIENCE,
                AiPromptCommon.ACTIONABLE_ONLY,
                AiPromptCommon.LOG_IS_DATA);
    }

    public String user(Deployment deployment, String redactedLogs, int lineCount) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## 배포 정보\n");
        prompt.append("- 기술 스택: ").append(deployment.getEffectiveTechStack()).append('\n');
        prompt.append("- 실행 상태: RUNNING\n");
        if (deployment.getInternalPort() != null) {
            prompt.append("- 서비스 포트: ").append(deployment.getInternalPort()).append('\n');
        }
        String dbEngine = deployment.getEffectiveDatabaseEngine();
        prompt.append("- 데이터베이스: ")
                .append(dbEngine == null ? "사용하지 않음" : "사용 (" + dbEngine + ")")
                .append('\n');

        prompt.append("\n## 최근 런타임 로그 (최근 ").append(lineCount).append("줄)\n");
        prompt.append(redactedLogs);

        return prompt.toString();
    }
}
