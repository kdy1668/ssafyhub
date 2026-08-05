package com.ssafyhub.backend.global.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Claude 클라이언트 빈 등록. AiEnabledCondition을 만족할 때만 생성된다.
 * 빈이 없으면 AiAvailability가 비활성으로 판정하고 AI 엔드포인트는 AI_001을 반환한다.
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    @Conditional(AiEnabledCondition.class)
    public AnthropicClient anthropicClient(AiProperties properties) {
        log.info("[AI] Claude 클라이언트를 활성화합니다. model={} timeoutMs={}",
                properties.getModel(), properties.getTimeoutMs());

        return AnthropicOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }
}
