package com.ssafyhub.backend.global.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 기능(배포 실패 진단 / 런타임 로그 이상 감지) 설정.
 *
 * SSAFY GMS(OpenAI 호환 프록시)를 호출한다. apiKey는 이 클래스가 유일한 통로이며,
 * "키가 주입되었는가"를 애플리케이션이 직접 판정해야 하므로(AiEnabledCondition)
 * 환경변수를 라이브러리가 알아서 읽는 방식을 쓰지 않는다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** AI 기능 사용 여부. 기본 false - 켜져 있어도 apiKey가 없으면 비활성이다. */
    private boolean enabled = false;

    /** GMS API 키. 비어 있으면 enabled 값과 무관하게 AI 기능이 비활성된다. */
    private String apiKey = "";

    /** OpenAI 호환 엔드포인트 base URL. 뒤에 /chat/completions를 붙여 호출한다. */
    private String baseUrl = "https://gms.ssafy.io/gmsapi/api.openai.com/v1";

    /**
     * 사용할 모델. GMS에서 gpt-4.1 / gpt-4o / gpt-4.1-mini / gpt-4o-mini 사용을 확인했다.
     * 구조화 출력(strict json_schema)을 쓰므로 이를 지원하는 모델이어야 한다.
     */
    private String model = "gpt-4.1";

    /** 응답 토큰 상한. 진단 결과가 잘리지 않을 정도면 충분하다. */
    private int maxTokens = 4000;

    private long connectTimeoutMs = 5000L;

    /** 모델 응답을 기다리는 시간. 진단 1건에 수십 초가 걸릴 수 있다. */
    private long readTimeoutMs = 60000L;

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
