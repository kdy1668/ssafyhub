package com.ssafyhub.backend.domain.deployment.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * 배포 실패 진단의 LLM 출력 스키마. 구조화 출력으로 이 형태가 강제된다.
 *
 * 필드 설명(@JsonPropertyDescription)이 그대로 JSON 스키마에 실려 모델에게 전달되므로,
 * 프롬프트와 중복되더라도 각 필드가 무엇인지 여기에 적어둔다.
 */
@JsonClassDescription("배포 실패 원인 진단 결과")
public record FailureDiagnosis(

        @JsonPropertyDescription("실패 원인을 전문용어 없이 한 문장으로. 예: 데이터베이스에 연결하지 못해 앱이 시작되지 못했습니다.")
        String summary,

        @JsonPropertyDescription("왜 그렇게 되었는지 비전공 교육생이 이해할 수 있게 설명. 용어를 쓸 경우 그 자리에서 풀어 쓴다.")
        String detail,

        @JsonPropertyDescription("사용자가 화면에서 실제로 수행할 수 있는 해결 절차. 순서대로.")
        List<Action> actions,

        @JsonPropertyDescription("판단의 근거가 된 로그 줄 번호. 전달받은 로그에 붙은 번호를 그대로 사용한다.")
        List<Integer> logLineNumbers,

        @JsonPropertyDescription("진단 확신도. 로그가 원인을 특정하기에 부족하면 LOW.")
        Confidence confidence
) {

    @JsonClassDescription("해결 절차 한 단계")
    public record Action(

            @JsonPropertyDescription("무엇을 해야 하는지 짧은 제목")
            String title,

            @JsonPropertyDescription("구체적으로 어떤 화면에서 무엇을 바꾸는지")
            String description
    ) {
    }

    public enum Confidence {
        HIGH, MEDIUM, LOW
    }
}
