package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.global.ai.AiJsonSchema;
import java.util.List;

/**
 * 런타임 로그 이상 감지의 모델 출력 스키마.
 *
 * Health에 UNKNOWN이 없는 것은 의도적이다. UNKNOWN은 "분석할 로그가 없다"는 뜻으로
 * 백엔드가 모델을 호출하지 않고 직접 채우는 값이다. 스키마에 넣으면 모델이 판단이 애매할 때
 * UNKNOWN으로 도피해, "정상이면 정상이라고 말한다"는 요구(FR-2.5)가 무너진다.
 */
public record RuntimeLogInsight(
        Health health,
        String summary,
        List<Finding> findings,
        List<String> nextSteps
) {

    public record Finding(Severity severity, String title, String detail, String suggestion) {
    }

    public enum Health {
        HEALTHY, WARNING, ERROR
    }

    public enum Severity {
        ERROR, WARNING, INFO
    }

    public static final AiJsonSchema<RuntimeLogInsight> SCHEMA = new AiJsonSchema<>(
            RuntimeLogInsight.class,
            "runtime_log_insight",
            """
            {
              "type": "object",
              "properties": {
                "health": {
                  "type": "string",
                  "description": "지금 앱 상태 판정. 문제가 보이지 않으면 HEALTHY.",
                  "enum": ["HEALTHY", "WARNING", "ERROR"]
                },
                "summary": {
                  "type": "string",
                  "description": "지금 앱이 어떤 상태인지 한두 문장으로"
                },
                "findings": {
                  "type": "array",
                  "description": "발견한 이상 징후. 문제가 없으면 빈 배열.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "severity": {
                        "type": "string",
                        "description": "심각도",
                        "enum": ["ERROR", "WARNING", "INFO"]
                      },
                      "title": { "type": "string", "description": "무엇이 문제인지 짧은 제목" },
                      "detail": {
                        "type": "string",
                        "description": "로그에서 확인 가능한 근거. 몇 번 나타났는지 등 사실을 쓴다."
                      },
                      "suggestion": { "type": "string", "description": "사용자가 확인해볼 수 있는 것" }
                    },
                    "required": ["severity", "title", "detail", "suggestion"],
                    "additionalProperties": false
                  }
                },
                "nextSteps": {
                  "type": "array",
                  "description": "권장 조치. 없으면 빈 배열.",
                  "items": { "type": "string" }
                }
              },
              "required": ["health", "summary", "findings", "nextSteps"],
              "additionalProperties": false
            }
            """);
}
