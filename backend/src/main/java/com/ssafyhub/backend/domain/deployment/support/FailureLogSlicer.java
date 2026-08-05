package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.domain.deployment.dto.DiagnosisLogLine;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 진단에 쓸 "실패 사이클" 로그 구간을 잘라낸다.
 *
 * deployment_logs는 재배포 이력이 한 테이블에 계속 누적된다. 전체를 보내면 이전 실패의
 * 스택트레이스가 섞여 진단이 엉키고 토큰도 낭비된다. 그래서 직전 종료 상태(RUNNING/FAILED)
 * 이후부터 이번 실패까지만 잘라 쓴다 - deploy-engine의 StaleDeploymentRecoveryService가
 * 재시도 횟수를 셀 때 쓰는 것과 같은 경계다.
 *
 * 잘라낸 줄은 그 자리에서 마스킹한다. 프롬프트와 화면 발췌가 완전히 같은 텍스트여야
 * logLineNumbers가 가리키는 줄이 어긋나지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FailureLogSlicer {

    private final SecretRedactor secretRedactor;

    /**
     * @param logsAsc  해당 배포의 전체 로그(id 오름차순)
     * @param endLogId 진단 대상 FAILED 로그의 id
     * @param maxLines 줄 수 상한. 초과하면 앞에서 잘라낸다.
     */
    public FailureLogSlice slice(List<DeploymentLog> logsAsc, Long endLogId, int maxLines) {
        int endIdx = indexOf(logsAsc, endLogId);
        if (endIdx < 0) {
            return new FailureLogSlice(List.of(), 0);
        }

        int startIdx = findCycleStart(logsAsc, endIdx);
        List<DeploymentLog> cycle = logsAsc.subList(startIdx, endIdx + 1);

        // 상한을 넘으면 뒤쪽(실패 지점에 가까운 줄)을 남긴다. 원인은 거의 항상 끝에 있다.
        int omitted = 0;
        if (maxLines > 0 && cycle.size() > maxLines) {
            omitted = cycle.size() - maxLines;
            cycle = cycle.subList(omitted, cycle.size());
        }

        List<DiagnosisLogLine> lines = new ArrayList<>(cycle.size());
        int lineNumber = 1;
        for (DeploymentLog log : cycle) {
            lines.add(new DiagnosisLogLine(
                    lineNumber++,
                    log.getStatus(),
                    secretRedactor.redact(log.getLogMessage())));
        }
        return new FailureLogSlice(List.copyOf(lines), omitted);
    }

    private int indexOf(List<DeploymentLog> logsAsc, Long logId) {
        for (int i = 0; i < logsAsc.size(); i++) {
            if (logsAsc.get(i).getId().equals(logId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 사이클 시작 인덱스를 찾는다. 역방향으로 스캔해 가장 가까운 PENDING을 사이클 시작으로 본다.
     *
     * "FAILED를 만나면 경계"로 하면 안 된다. deploy-engine은 실패 시 파드 로그와 스택트레이스를
     * 한 줄씩 모두 FAILED 상태로 기록하므로(DeploymentProcessor의 실패 진단 경로), 마지막 FAILED
     * 바로 앞줄도 FAILED다. 그 규칙이면 첫 줄에서 멈춰 정작 원인이 담긴 줄들을 전부 버리게 된다.
     * 실제 데이터로 확인한 버그다.
     *
     * PENDING을 기준으로 삼는 근거는 사이클이 항상 PENDING으로 시작한다는 점이다.
     * 신규 배포는 DeploymentService가, 재배포는 deploy-engine이 PENDING을 남긴다
     * (StaleDeploymentRecoveryService가 PENDING 개수로 재시도 횟수를 세는 것과 같은 전제).
     *
     * PENDING 없이 RUNNING을 먼저 만나면 이전 사이클이 성공으로 끝난 지점이므로 그 다음 줄부터다.
     */
    private int findCycleStart(List<DeploymentLog> logsAsc, int endIdx) {
        for (int i = endIdx; i >= 0; i--) {
            DeploymentStatus status = logsAsc.get(i).getStatus();
            if (status == DeploymentStatus.PENDING) {
                return i;
            }
            if (i < endIdx && status == DeploymentStatus.RUNNING) {
                return i + 1;
            }
        }
        return 0;
    }
}
