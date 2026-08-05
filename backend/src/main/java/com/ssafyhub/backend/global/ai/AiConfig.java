package com.ssafyhub.backend.global.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * GMS(OpenAI 호환) 호출용 클라이언트 등록. AiEnabledCondition을 만족할 때만 생성된다.
 * 빈이 없으면 AiAvailability가 비활성으로 판정하고 AI 엔드포인트는 AI_001을 반환한다.
 *
 * 별도 SDK를 쓰지 않고 Spring RestClient를 사용한다. 프로젝트가 이미 deploy-engine 호출에
 * 같은 방식을 쓰고 있어(DevOpsConfig) 일관되고, 새 의존성을 추가하지 않아도 된다.
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    @Conditional(AiEnabledCondition.class)
    public AiHttpClient aiHttpClient(RestClient.Builder builder, AiProperties properties) {
        log.info("[AI] AI 기능을 활성화합니다. baseUrl={} model={} readTimeoutMs={}",
                properties.getBaseUrl(), properties.getModel(), properties.getReadTimeoutMs());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        RestClient restClient = builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return new AiHttpClient(restClient);
    }
}
