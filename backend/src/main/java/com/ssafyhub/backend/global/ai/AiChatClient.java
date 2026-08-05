package com.ssafyhub.backend.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * GMS(OpenAI 호환) 단일 호출 창구. 구조화 출력만 사용한다.
 *
 * 자유 텍스트를 받아 파싱하지 않는 이유는, 실패했을 때 절반만 채워진 결과가 남는 것이
 * 결과가 없는 것보다 나쁘기 때문이다. 스키마 위반은 전부 AI_CALL_FAILED로 처리하고
 * 부분 결과를 반환하지 않는다.
 *
 * 애플리케이션 레벨 재시도를 하지 않는다. 비용 상한이 "실패 1건당 1회" /
 * "30초당 1회"로 설계되어 있어 재시도가 그 상한을 무너뜨린다.
 */
@Component
@RequiredArgsConstructor
public class AiChatClient {

    private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final ObjectProvider<AiHttpClient> clientProvider;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 구조화 출력으로 모델을 호출한다.
     *
     * @param schema       출력 스키마와 역직렬화 타입
     * @param systemPrompt 역할·독자·제약
     * @param userPrompt   분석 대상 데이터
     * @param purpose      로그 태그 (FAILURE_DIAGNOSIS / RUNTIME_LOG_DETECTION)
     * @param targetId     로그에 남길 대상 리소스 id (배포 id 등)
     * @throws BusinessException AI_DISABLED - 기능 비활성 / AI_CALL_FAILED - 호출·검증 실패
     */
    public <T> AiResult<T> ask(
            AiJsonSchema<T> schema,
            String systemPrompt,
            String userPrompt,
            String purpose,
            Long targetId
    ) {
        AiHttpClient http = clientProvider.getIfAvailable();
        if (http == null) {
            throw new BusinessException(ErrorCode.AI_DISABLED);
        }

        long startedAt = System.currentTimeMillis();
        try {
            JsonNode response = http.client().post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .body(requestBody(schema, systemPrompt, userPrompt))
                    .retrieve()
                    .body(JsonNode.class);

            return parse(schema, response, purpose, targetId, startedAt);

        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // 타임아웃 / 4xx / 5xx / 본문 파싱 실패가 모두 여기로 온다.
            // 예외 종류는 로그로만 남기고 사용자에게는 동일한 안내를 준다.
            logCall(purpose, targetId, 0, 0, startedAt,
                    "FAILED(" + exception.getClass().getSimpleName() + ")");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    private <T> Map<String, Object> requestBody(
            AiJsonSchema<T> schema, String systemPrompt, String userPrompt) {
        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("name", schema.name());
        // strict를 켜면 모델이 스키마를 벗어난 응답을 만들 수 없다. 파싱 실패 가능성을
        // 프롬프트 품질에 의존하지 않게 하는 것이 이 기능의 신뢰성에 중요하다.
        jsonSchema.put("strict", true);
        jsonSchema.put("schema", readSchema(schema));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        body.put("response_format", Map.of("type", "json_schema", "json_schema", jsonSchema));
        return body;
    }

    private JsonNode readSchema(AiJsonSchema<?> schema) {
        try {
            return objectMapper.readTree(schema.schema());
        } catch (Exception exception) {
            // 스키마 상수 자체가 잘못된 JSON이라는 뜻이다. 설정 오류에 가깝다.
            log.error("[AI] JSON 스키마가 올바르지 않습니다. name={}", schema.name(), exception);
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    private <T> AiResult<T> parse(
            AiJsonSchema<T> schema, JsonNode response, String purpose, Long targetId, long startedAt) {
        if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
            logCall(purpose, targetId, 0, 0, startedAt, "NO_CHOICES");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }

        JsonNode choice = response.get("choices").get(0);
        JsonNode message = choice.path("message");
        int inputTokens = response.path("usage").path("prompt_tokens").asInt(0);
        int outputTokens = response.path("usage").path("completion_tokens").asInt(0);
        String model = response.path("model").asText(properties.getModel());

        // 안전 정책상 거부. content가 비거나 스키마를 지키지 않으므로 먼저 걸러낸다.
        if (message.hasNonNull("refusal")) {
            logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "REFUSAL");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }

        // 토큰 상한에 걸려 끊긴 응답은 JSON이 미완성이라 파싱도 실패한다.
        // 원인을 구분해 남겨야 max-tokens를 올려야 하는 상황임을 알 수 있다.
        String finishReason = choice.path("finish_reason").asText("");
        if ("length".equals(finishReason)) {
            logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "TRUNCATED");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }

        String content = message.path("content").asText("");
        if (content.isBlank()) {
            logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "EMPTY_CONTENT");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }

        T value;
        try {
            value = objectMapper.readValue(content, schema.type());
        } catch (Exception exception) {
            logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "SCHEMA_MISMATCH");
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }

        logCall(purpose, targetId, inputTokens, outputTokens, startedAt, "SUCCESS");
        return new AiResult<>(value, model, inputTokens, outputTokens);
    }

    /**
     * 사용량 기록. 프롬프트와 응답 본문은 절대 남기지 않는다 - 사용자 로그에 비밀값이
     * 섞여 있을 수 있고, 마스킹은 전송 대상에만 적용되기 때문이다(NFR-5.2).
     */
    private void logCall(String purpose, Long targetId, int inputTokens, int outputTokens,
            long startedAt, String result) {
        log.info("[AI] purpose={} targetId={} model={} inputTokens={} outputTokens={} elapsedMs={} result={}",
                purpose, targetId, properties.getModel(), inputTokens, outputTokens,
                System.currentTimeMillis() - startedAt, result);
    }
}
