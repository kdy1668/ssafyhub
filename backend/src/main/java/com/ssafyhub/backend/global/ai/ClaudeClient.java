package com.ssafyhub.backend.global.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Claude 호출 단일 창구. 구조화 출력만 사용한다.
 *
 * 자유 텍스트를 받아 파싱하지 않는 이유는, 실패했을 때 절반만 채워진 결과가 남는 것이
 * 결과가 없는 것보다 나쁘기 때문이다. 스키마 위반은 전부 AI_CALL_FAILED로 처리하고
 * 부분 결과를 반환하지 않는다.
 *
 * 애플리케이션 레벨 재시도를 하지 않는다(SDK 기본 재시도에만 의존). 비용 상한이
 * "실패 1건당 1회" / "30초당 1회"로 설계되어 있어 재시도가 그 상한을 무너뜨린다.
 */
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final ObjectProvider<AnthropicClient> clientProvider;
    private final AiProperties properties;

    /**
     * 구조화 출력으로 Claude를 호출한다.
     *
     * @param responseType 출력 스키마를 정의하는 record. 이 타입으로 응답이 강제된다.
     * @param systemPrompt 역할·독자·제약
     * @param userPrompt   분석 대상 데이터
     * @param purpose      로그 태그 (FAILURE_DIAGNOSIS / RUNTIME_LOG_DETECTION)
     * @param targetId     로그에 남길 대상 리소스 id (배포 id 등)
     * @throws BusinessException AI_DISABLED - 기능 비활성 / AI_CALL_FAILED - 호출·검증 실패
     */
    public <T> AiResult<T> ask(
            Class<T> responseType,
            String systemPrompt,
            String userPrompt,
            String purpose,
            Long targetId
    ) {
        AnthropicClient client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(ErrorCode.AI_DISABLED);
        }

        StructuredMessageCreateParams<T> params = com.anthropic.models.messages.MessageCreateParams.builder()
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .outputConfig(responseType)
                .build();

        long startedAt = System.currentTimeMillis();
        try {
            StructuredMessage<T> message = client.messages().create(params);

            // stop_reason이 refusal이면 content가 비어 있거나 스키마를 지키지 않는다.
            // content를 먼저 읽으면 원인을 알 수 없는 파싱 실패로 보이므로 여기서 먼저 걸러낸다.
            Optional<StopReason> stopReason = message.stopReason();
            if (stopReason.isPresent() && StopReason.REFUSAL.equals(stopReason.get())) {
                logCall(purpose, targetId, message.usage().inputTokens(),
                        message.usage().outputTokens(), startedAt, "REFUSAL");
                throw new BusinessException(ErrorCode.AI_CALL_FAILED);
            }

            T value = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            int inputTokens = (int) message.usage().inputTokens();
            int outputTokens = (int) message.usage().outputTokens();

            if (value == null) {
                logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "NO_CONTENT");
                throw new BusinessException(ErrorCode.AI_CALL_FAILED);
            }

            logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "SUCCESS");
            return new AiResult<>(value, message.model().toString(), inputTokens, outputTokens);

        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // 타임아웃 / 5xx / 429 / 스키마 역직렬화 실패가 모두 여기로 온다.
            // 예외 종류는 로그로만 남기고 사용자에게는 동일한 안내를 준다.
            logCall(purpose, targetId, 0, 0, startedAt,
                    "FAILED(" + exception.getClass().getSimpleName() + ")");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    /**
     * 사용량 기록. 프롬프트와 응답 본문은 절대 남기지 않는다 - 사용자 로그에 비밀값이
     * 섞여 있을 수 있고, 마스킹은 전송 대상에만 적용되기 때문이다(NFR-5.2).
     */
    private void logCall(String purpose, Long targetId, long inputTokens, long outputTokens,
            long startedAt, String result) {
        log.info("[AI] purpose={} targetId={} model={} inputTokens={} outputTokens={} elapsedMs={} result={}",
                purpose, targetId, properties.getModel(), inputTokens, outputTokens,
                System.currentTimeMillis() - startedAt, result);
    }
}
