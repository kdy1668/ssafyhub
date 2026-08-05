package com.ssafyhub.backend.global.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 기능(배포 실패 진단 / 런타임 로그 이상 감지) 설정.
 *
 * apiKey는 이 클래스가 유일한 통로다. SDK의 환경변수 자동 인식(fromEnv)을 쓰지 않는 이유는
 * "키가 주입되었는가"를 애플리케이션이 직접 판정해야 하기 때문이다(AiEnabledCondition).
 * 키가 없으면 Claude 클라이언트 빈 자체를 만들지 않고, 기동은 정상 진행한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** AI 기능 사용 여부. 기본 false - 켜져 있어도 apiKey가 없으면 비활성이다. */
    private boolean enabled = false;

    /** Claude API 키. 비어 있으면 enabled 값과 무관하게 AI 기능이 비활성된다. */
    private String apiKey = "";

    private String model = "claude-opus-5";

    /** 응답 토큰 상한. 비스트리밍 호출이라 HTTP 타임아웃에 걸리지 않을 범위로 둔다. */
    private int maxTokens = 16000;

    private long timeoutMs = 60000L;

    private FailureDiagnosis failureDiagnosis = new FailureDiagnosis();

    private RuntimeLogDetection runtimeLogDetection = new RuntimeLogDetection();

    @Getter
    @Setter
    public static class FailureDiagnosis {

        /** 진단에 사용할 실패 사이클 로그 최대 줄 수. 초과분은 앞부분을 잘라낸다. */
        private int maxLogLines = 400;
    }

    @Getter
    @Setter
    public static class RuntimeLogDetection {

        /** 배포당 최소 호출 간격(초). */
        private int cooldownSeconds = 30;

        /** deploy-engine에서 가져올 런타임 로그 줄 수. */
        private int tail = 300;
    }
}
