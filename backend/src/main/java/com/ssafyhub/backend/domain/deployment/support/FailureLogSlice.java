package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DiagnosisLogLine;
import java.util.List;

/**
 * 진단에 쓸 실패 사이클 로그 구간.
 *
 * @param lines               마스킹되고 1부터 번호가 붙은 로그 줄
 * @param omittedLeadingLines 줄 수 상한 때문에 앞에서 잘라낸 줄 수 (0이면 전체 포함)
 */
public record FailureLogSlice(List<DiagnosisLogLine> lines, int omittedLeadingLines) {

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
