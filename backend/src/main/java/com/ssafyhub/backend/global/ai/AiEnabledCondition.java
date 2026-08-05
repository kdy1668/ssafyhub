package com.ssafyhub.backend.global.ai;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Claude 클라이언트 빈을 만들 조건: ai.enabled=true 그리고 ai.api-key가 비어 있지 않음.
 *
 * @ConditionalOnProperty로 enabled만 보면 "켜뒀지만 키를 안 넣은" 상태에서 클라이언트가
 * 만들어지고 첫 호출에서야 실패한다. 반대로 빈 생성 시점에 예외를 던지면 애플리케이션이
 * 기동하지 못한다 - 둘 다 요구사항(FR-C.1: 키가 없으면 AI 기능만 조용히 비활성)에 어긋나서
 * Condition으로 판정한다.
 */
public class AiEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = Boolean.parseBoolean(
                context.getEnvironment().getProperty("ai.enabled", "false"));
        String apiKey = context.getEnvironment().getProperty("ai.api-key", "");

        return enabled && StringUtils.hasText(apiKey);
    }
}
