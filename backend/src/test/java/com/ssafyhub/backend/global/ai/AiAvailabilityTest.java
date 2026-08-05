package com.ssafyhub.backend.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 기능 토글(FR-C.1) 검증.
 *
 * 핵심은 "키가 없으면 enabled=true여도 기동이 실패하지 않고 조용히 비활성된다"는 것이다.
 * 전체 컨텍스트를 띄우지 않고 ApplicationContextRunner로 필요한 빈만 올려 확인한다.
 */
class AiAvailabilityTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(AiTestConfig.class);

    @Test
    @DisplayName("기본 설정(키 없음, enabled 미지정)에서는 비활성이고 기동에 실패하지 않는다")
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AiHttpClient.class);
            assertThat(context.getBean(AiAvailability.class).isAvailable()).isFalse();
        });
    }

    @Test
    @DisplayName("enabled=true인데 키가 없으면 비활성이고 기동에 실패하지 않는다")
    void disabledWhenApiKeyMissing() {
        runner.withPropertyValues("ai.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AiHttpClient.class);
                    assertThat(context.getBean(AiAvailability.class).isAvailable()).isFalse();
                });
    }

    @Test
    @DisplayName("키가 있어도 enabled=false면 비활성이다")
    void disabledWhenToggleOff() {
        runner.withPropertyValues("ai.enabled=false", "ai.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AiHttpClient.class);
                    assertThat(context.getBean(AiAvailability.class).isAvailable()).isFalse();
                });
    }

    @Test
    @DisplayName("enabled=true이고 키가 있으면 활성되고 클라이언트 빈이 생성된다")
    void enabledWhenToggleOnAndApiKeyPresent() {
        runner.withPropertyValues("ai.enabled=true", "ai.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AiHttpClient.class);
                    assertThat(context.getBean(AiAvailability.class).isAvailable()).isTrue();
                });
    }

    @Test
    @DisplayName("설정 기본값이 GMS(OpenAI 호환) 기준과 일치한다")
    void bindsDefaultProperties() {
        runner.run(context -> {
            AiProperties properties = context.getBean(AiProperties.class);

            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getBaseUrl()).isEqualTo("https://gms.ssafy.io/gmsapi/api.openai.com/v1");
            assertThat(properties.getModel()).isEqualTo("gpt-4.1");
            assertThat(properties.getMaxTokens()).isEqualTo(4000);
            assertThat(properties.getReadTimeoutMs()).isEqualTo(60000L);
            assertThat(properties.getFailureDiagnosis().getMaxLogLines()).isEqualTo(400);
            assertThat(properties.getRuntimeLogDetection().getCooldownSeconds()).isEqualTo(30);
            assertThat(properties.getRuntimeLogDetection().getTail()).isEqualTo(300);
        });
    }

    @Test
    @DisplayName("비활성 상태에서 호출하면 AI_001로 거부하고 네트워크를 타지 않는다")
    void chatClientRejectsWhenDisabled() {
        runner.run(context -> {
            AiChatClient client = context.getBean(AiChatClient.class);
            AiJsonSchema<String> schema = new AiJsonSchema<>(String.class, "dummy", "{}");

            BusinessException exception = catchThrowableOfType(
                    () -> client.ask(schema, "sys", "user", "TEST", 1L),
                    BusinessException.class);

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_DISABLED);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiConfig.class, AiAvailability.class, AiChatClient.class, SecretRedactor.class})
    static class AiTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
