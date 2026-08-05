package com.ssafyhub.backend.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.deployment.dto.FailureDiagnosis;
import com.ssafyhub.backend.domain.deployment.dto.RuntimeLogInsight;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

/**
 * 실제 GMS를 호출하는 통합 테스트. 기본적으로 건너뛴다.
 *
 * 실행 방법:
 * <pre>
 *   AI_IT_ENABLED=true \
 *   OPENAI_API_KEY=&lt;GMS 키&gt; \
 *   OPENAI_BASE_URL=https://gms.ssafy.io/gmsapi/api.openai.com/v1 \
 *   ./gradlew test --tests '*AiChatClientGmsIntegrationTest'
 * </pre>
 *
 * 키를 파일에 적지 않는다. 환경변수로만 받고, 없으면 테스트 자체가 비활성된다.
 * CI에서는 키를 넣지 않으므로 자동으로 건너뛴다.
 *
 * 이 테스트가 검증하는 것은 단위 테스트로 절대 잡을 수 없는 것들이다.
 * 요청 본문 형식, strict json_schema가 실제로 통과하는지, 응답이 record로
 * 역직렬화되는지, 토큰 사용량 필드 위치.
 */
@EnabledIfEnvironmentVariable(named = "AI_IT_ENABLED", matches = "true")
class AiChatClientGmsIntegrationTest {

    private final AiChatClient client = buildClient();

    @Test
    @DisplayName("실패 로그로 진단을 받아 스키마대로 파싱된다")
    void diagnosesRealFailureLog() {
        String system = """
                당신은 SSAFY Hub 배포 플랫폼의 배포 실패 진단 도우미입니다.
                읽는 사람은 비전공 교육생입니다. 용어를 쓸 경우 그 자리에서 풀어 쓰세요.
                근거가 된 로그 줄 번호를 logLineNumbers에 반드시 담으세요.
                SSH 접속이나 kubectl 실행처럼 사용자가 할 수 없는 조치는 제안하지 마세요.""";
        String user = """
                ## 배포 설정
                - 기술 스택: SPRING_BOOT
                - 데이터베이스: 사용 (MYSQL)
                - DB 접속 환경변수 이름: DB_HOST, DB_PORT

                ## 배포 진행 로그
                1 | PENDING | 배포 요청이 접수되었습니다.
                2 | BUILDING | 이미지 빌드 완료
                3 | DEPLOYING | 앱 기동 대기 중
                4 | FAILED | Caused by: com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
                5 | FAILED | jdbc:mysql://localhost:3306/mydb 로 접속 시도했습니다.
                6 | FAILED | 상태 변경: FAILED""";

        AiResult<FailureDiagnosis> result =
                client.ask(FailureDiagnosis.SCHEMA, system, user, "IT_FAILURE_DIAGNOSIS", 1L);

        FailureDiagnosis diagnosis = result.value();
        assertThat(diagnosis.summary()).isNotBlank();
        assertThat(diagnosis.detail()).isNotBlank();
        assertThat(diagnosis.actions()).isNotEmpty();
        assertThat(diagnosis.actions()).allSatisfy(action -> {
            assertThat(action.title()).isNotBlank();
            assertThat(action.description()).isNotBlank();
        });
        assertThat(diagnosis.confidence()).isNotNull();

        // 근거 줄 번호가 전달한 범위(1~6) 안에 있어야 한다. 범위를 벗어나면
        // 화면에서 가리킬 줄이 없어 근거 표시가 무의미해진다.
        assertThat(diagnosis.logLineNumbers()).isNotEmpty();
        assertThat(diagnosis.logLineNumbers()).allSatisfy(
                lineNumber -> assertThat(lineNumber).isBetween(1, 6));

        assertThat(result.inputTokens()).isPositive();
        assertThat(result.outputTokens()).isPositive();
        assertThat(result.model()).isNotBlank();

        System.out.printf("[IT] model=%s tokens=%d/%d confidence=%s lines=%s%n",
                result.model(), result.inputTokens(), result.outputTokens(),
                diagnosis.confidence(), diagnosis.logLineNumbers());
        System.out.println("[IT] summary: " + diagnosis.summary());
    }

    @Test
    @DisplayName("정상 런타임 로그에는 문제를 만들어내지 않는다")
    void reportsHealthyForNormalLogs() {
        String system = """
                당신은 실행 중인 애플리케이션의 로그를 읽고 이상 징후를 찾아내는 도우미입니다.
                정상적으로 동작하는 것처럼 보이면 health를 HEALTHY로 두고 findings를 비우세요.
                문제를 만들어내지 마세요. 프레임워크가 기동 중 출력하는 일반적인 경고나
                INFO 로그를 이상 징후로 보고하지 마세요.""";
        String user = """
                ## 배포 정보
                - 기술 스택: SPRING_BOOT
                - 실행 상태: RUNNING

                ## 최근 런타임 로그 (최근 5줄)
                2026-08-05T14:38:00.101Z  INFO  Starting Application using Java 21
                2026-08-05T14:38:02.113Z  INFO  Tomcat initialized with port 8080 (http)
                2026-08-05T14:38:05.204Z  INFO  HikariPool-1 - Start completed.
                2026-08-05T14:38:06.310Z  INFO  Started Application in 6.2 seconds
                2026-08-05T14:39:11.002Z  INFO  GET /api/health 200 3ms""";

        AiResult<RuntimeLogInsight> result =
                client.ask(RuntimeLogInsight.SCHEMA, system, user, "IT_RUNTIME_LOG", 1L);

        RuntimeLogInsight insight = result.value();
        assertThat(insight.health()).isEqualTo(RuntimeLogInsight.Health.HEALTHY);
        assertThat(insight.findings()).isEmpty();
        assertThat(insight.summary()).isNotBlank();

        System.out.printf("[IT] health=%s findings=%d | %s%n",
                insight.health(), insight.findings().size(), insight.summary());
    }

    @Test
    @DisplayName("오류가 반복되는 로그는 이상으로 판정한다")
    void detectsRepeatedErrors() {
        String system = """
                당신은 실행 중인 애플리케이션의 로그를 읽고 이상 징후를 찾아내는 도우미입니다.
                로그에 반복해서 나타나는 패턴과 명확한 오류에 집중하세요.
                findings의 detail에는 몇 번 나타났는지처럼 로그에서 확인 가능한 사실을 쓰세요.""";
        StringBuilder logs = new StringBuilder("## 최근 런타임 로그 (최근 12줄)\n");
        logs.append("2026-08-05T15:00:00Z INFO Started Application in 6.2 seconds\n");
        for (int i = 1; i <= 11; i++) {
            logs.append("2026-08-05T15:0").append(i % 10)
                    .append(":00Z ERROR HikariPool-1 - Connection is not available,"
                            + " request timed out after 30000ms\n");
        }

        AiResult<RuntimeLogInsight> result =
                client.ask(RuntimeLogInsight.SCHEMA, system, logs.toString(), "IT_RUNTIME_LOG", 1L);

        RuntimeLogInsight insight = result.value();
        assertThat(insight.health()).isIn(RuntimeLogInsight.Health.WARNING, RuntimeLogInsight.Health.ERROR);
        assertThat(insight.findings()).isNotEmpty();
        assertThat(insight.findings()).allSatisfy(finding -> {
            assertThat(finding.severity()).isNotNull();
            assertThat(finding.title()).isNotBlank();
            assertThat(finding.detail()).isNotBlank();
        });

        System.out.printf("[IT] health=%s findings=%d | %s%n",
                insight.health(), insight.findings().size(), insight.findings().get(0).title());
    }

    /**
     * 프로덕션과 같은 경로로 클라이언트를 만든다. AiConfig를 직접 호출하므로
     * 요청 팩토리·헤더·타임아웃 구성까지 실제와 동일하다.
     */
    private static AiChatClient buildClient() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey(requireEnv("OPENAI_API_KEY"));
        String baseUrl = System.getenv("OPENAI_BASE_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            properties.setBaseUrl(baseUrl);
        }

        AiHttpClient httpClient = new AiConfig().aiHttpClient(RestClient.builder(), properties);

        return new AiChatClient(singleton(httpClient), properties, new ObjectMapper());
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " 환경변수가 필요합니다. 키를 파일에 적지 말고 환경변수로 전달하세요.");
        }
        return value;
    }

    /** AiChatClient가 ObjectProvider로 클라이언트를 받으므로 최소 구현을 만들어 넘긴다. */
    private static ObjectProvider<AiHttpClient> singleton(AiHttpClient instance) {
        return new ObjectProvider<>() {
            @Override
            public AiHttpClient getObject() {
                return instance;
            }

            @Override
            public AiHttpClient getObject(Object... args) {
                return instance;
            }

            @Override
            public AiHttpClient getIfAvailable() {
                return instance;
            }

            @Override
            public AiHttpClient getIfUnique() {
                return instance;
            }
        };
    }
}
