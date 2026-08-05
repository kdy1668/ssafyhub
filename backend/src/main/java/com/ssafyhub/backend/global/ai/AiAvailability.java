package com.ssafyhub.backend.global.ai;

import com.anthropic.client.AnthropicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * AI 기능 활성 여부 판정.
 *
 * 설정값을 다시 읽지 않고 "Claude 클라이언트 빈이 존재하는가"만 본다.
 * 판정 기준이 AiEnabledCondition 한 곳에만 있어야 설정과 실제 동작이 어긋나지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AiAvailability {

    private final ObjectProvider<AnthropicClient> clientProvider;

    public boolean isAvailable() {
        return clientProvider.getIfAvailable() != null;
    }
}
