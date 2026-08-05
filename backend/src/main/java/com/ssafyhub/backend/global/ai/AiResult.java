package com.ssafyhub.backend.global.ai;

/**
 * Claude 호출 결과. 파싱된 값과 함께 사용량을 돌려준다.
 * 배포 실패 진단은 사용량을 DB(deployment_diagnoses)에 남겨야 하므로 반환 타입에 포함한다.
 *
 * @param value        스키마에 맞게 파싱된 응답
 * @param model        실제 응답을 생성한 모델
 * @param inputTokens  입력 토큰 수
 * @param outputTokens 출력 토큰 수
 */
public record AiResult<T>(T value, String model, int inputTokens, int outputTokens) {
}
