package com.ssafyhub.backend.global.ai;

import org.springframework.web.client.RestClient;

/**
 * GMS 호출용 RestClient를 감싸는 전용 타입.
 *
 * 이 래퍼가 필요한 이유는 주입 모호성 때문이다. 프로젝트에는 이미 deploy-engine용
 * RestClient 빈(devOpsRestClient)이 있어서, ObjectProvider&lt;RestClient&gt;로 받으면
 * 후보가 둘이 되어 해석에 실패한다. 전용 타입으로 감싸면 모호성이 사라지고,
 * "AI가 비활성이면 이 빈이 없다"는 판정도 그대로 유지된다.
 */
public record AiHttpClient(RestClient client) {
}
