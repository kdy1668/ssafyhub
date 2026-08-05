package com.ssafyhub.backend.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

/**
 * "토글은 켰는데 키가 없는" 배포 조합에서 애플리케이션이 기동하는지 검증한다.
 *
 * 이 조합은 실수가 아니라 실제로 발생하는 순서다 - Jenkinsfile에 AI_ENABLED를 넣는 커밋이
 * 먼저 배포되고 크리덴셜 등록이 뒤따르거나, 크리덴셜 ID를 잘못 적으면 키가 빈 문자열로
 * 주입된다. 여기서 컨텍스트가 실패하면 AI와 무관한 프로덕션 전체가 내려간다.
 * SECRET_ENCRYPTION_KEY 누락으로 기동이 실패한 전례가 있어 회귀 방지로 고정해 둔다.
 *
 * AiFeatureBeanWiringTest는 AiAvailability와 AiChatClient를 목으로 대체하므로 이 경로를
 * 검증하지 못한다. 그래서 여기서는 AiConfig와 AiEnabledCondition을 실제로 태운다.
 */
class AiConfigKeyMissingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("토글만 켜고 키가 없으면 기동은 성공하고 AI 기능만 비활성이다")
    void startsUpWhenKeyIsMissing() {
        runner.withPropertyValues("ai.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AiHttpClient.class);
            assertThat(context.getBean(AiAvailability.class).isAvailable()).isFalse();
        });
    }

    @Test
    @DisplayName("키가 공백뿐이면 없는 것으로 본다")
    void treatsBlankKeyAsMissing() {
        runner.withPropertyValues("ai.enabled=true", "ai.api-key=   ").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AiHttpClient.class);
            assertThat(context.getBean(AiAvailability.class).isAvailable()).isFalse();
        });
    }

    @Test
    @DisplayName("키만 넣고 토글을 켜지 않으면 활성되지 않는다")
    void staysDisabledWhenToggleIsOff() {
        runner.withPropertyValues("ai.api-key=dummy-key").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AiHttpClient.class);
            assertThat(context.getBean(AiAvailability.class).isAvailable()).isFalse();
        });
    }

    @Test
    @DisplayName("토글과 키가 모두 있을 때만 클라이언트가 등록된다")
    void registersClientOnlyWhenBothPresent() {
        runner.withPropertyValues("ai.enabled=true", "ai.api-key=dummy-key").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AiHttpClient.class);
            assertThat(context.getBean(AiAvailability.class).isAvailable()).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiConfig.class, AiAvailability.class})
    static class TestConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }
}
