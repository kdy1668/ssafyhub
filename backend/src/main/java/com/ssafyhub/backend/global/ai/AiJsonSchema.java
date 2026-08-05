package com.ssafyhub.backend.global.ai;

/**
 * 구조화 출력에 쓸 JSON 스키마와 그것을 역직렬화할 타입을 함께 묶는다.
 *
 * 스키마를 record에서 자동 생성하지 않고 손으로 적는다. OpenAI의 strict 모드는
 * 모든 object에 additionalProperties: false와 전 필드 required를 요구하는 등
 * 제약이 까다로워, 자동 생성기가 만든 스키마가 조용히 거부되는 편이 더 찾기 어렵다.
 *
 * 손으로 적으면 record와 스키마가 어긋날 위험이 생기므로, AiJsonSchemaConsistencyTest가
 * 스키마의 필드 이름과 record 컴포넌트 이름이 같은지 검증한다.
 *
 * @param type   응답을 담을 타입
 * @param name   스키마 이름(OpenAI가 요구하는 식별자, snake_case)
 * @param schema JSON Schema 본문
 */
public record AiJsonSchema<T>(Class<T> type, String name, String schema) {
}
