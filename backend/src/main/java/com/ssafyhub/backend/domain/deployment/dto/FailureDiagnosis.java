package com.ssafyhub.backend.domain.deployment.dto;

import com.ssafyhub.backend.global.ai.AiJsonSchema;
import java.util.List;

/**
 * 배포 실패 진단의 모델 출력 스키마. 구조화 출력(strict json_schema)으로 이 형태가 강제된다.
 *
 * 필드 설명은 스키마의 description에 적는다 - 그것이 실제로 모델에게 전달되는 경로다.
 * 자바 애노테이션에 적어도 요청 본문에는 실리지 않는다.
 */
public record FailureDiagnosis(
        String summary,
        String detail,
        List<Action> actions,
        List<Integer> logLineNumbers,
        Confidence confidence
) {

    public record Action(String title, String description) {
    }

    public enum Confidence {
        HIGH, MEDIUM, LOW
    }

    /**
     * strict 모드 요구사항: 모든 object에 additionalProperties: false,
     * 그리고 properties의 모든 키가 required에 있어야 한다. 하나라도 빠지면 400이다.
     */
    public static final AiJsonSchema<FailureDiagnosis> SCHEMA = new AiJsonSchema<>(
            FailureDiagnosis.class,
            "failure_diagnosis",
            """
            {
              "type": "object",
              "properties": {
                "summary": {
                  "type": "string",
                  "description": "실패 원인을 전문용어 없이 한 문장으로. 예: 데이터베이스에 연결하지 못해 앱이 시작되지 못했습니다."
                },
                "detail": {
                  "type": "string",
                  "description": "왜 그렇게 되었는지 비전공 교육생이 이해할 수 있게 설명. 용어를 쓸 경우 그 자리에서 풀어 쓴다."
                },
                "actions": {
                  "type": "array",
                  "description": "사용자가 화면에서 실제로 수행할 수 있는 해결 절차. 순서대로.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "title": { "type": "string", "description": "무엇을 해야 하는지 짧은 제목" },
                      "description": { "type": "string", "description": "구체적으로 어떤 화면에서 무엇을 바꾸는지" }
                    },
                    "required": ["title", "description"],
                    "additionalProperties": false
                  }
                },
                "logLineNumbers": {
                  "type": "array",
                  "description": "판단의 근거가 된 로그 줄 번호. 전달받은 로그에 붙은 번호를 그대로 사용한다.",
                  "items": { "type": "integer" }
                },
                "confidence": {
                  "type": "string",
                  "description": "진단 확신도. 로그가 원인을 특정하기에 부족하면 LOW.",
                  "enum": ["HIGH", "MEDIUM", "LOW"]
                }
              },
              "required": ["summary", "detail", "actions", "logLineNumbers", "confidence"],
              "additionalProperties": false
            }
            """);
}
