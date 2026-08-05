package com.ssafyhub.backend.domain.deployment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafyhub.backend.domain.deployment.dto.DiagnosisLogLine;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 실패 사이클 구간 추출 검증.
 *
 * 여기서 가장 중요한 건 "이전 재배포의 로그가 섞이지 않는지"다. 섞이면 AI가 이미 해결된
 * 옛 오류를 원인으로 지목해 사용자를 엉뚱한 곳으로 보낸다.
 */
class FailureLogSlicerTest {

    private final FailureLogSlicer slicer = new FailureLogSlicer(new SecretRedactor());

    @Test
    @DisplayName("직전 FAILED 이후부터 이번 FAILED까지만 잘라낸다")
    void slicesOnlyCurrentCycleAfterPreviousFailure() {
        List<DeploymentLog> logs = logs(
                log(1520L, DeploymentStatus.PENDING, "이전 사이클 접수"),
                log(1533L, DeploymentStatus.BUILDING, "이전 사이클 빌드"),
                log(1544L, DeploymentStatus.FAILED, "이전 사이클 실패"),
                log(1551L, DeploymentStatus.PENDING, "이번 접수"),
                log(1560L, DeploymentStatus.BUILDING, "이번 빌드"),
                log(1573L, DeploymentStatus.FAILED, "이번 실패"));

        FailureLogSlice slice = slicer.slice(logs, 1573L, 400);

        assertThat(slice.lines()).extracting(DiagnosisLogLine::message)
                .containsExactly("이번 접수", "이번 빌드", "이번 실패");
        assertThat(slice.omittedLeadingLines()).isZero();
    }

    @Test
    @DisplayName("실패 사이클이 FAILED를 여러 줄 남겨도 원인 줄을 모두 포함한다")
    void includesAllFailedLinesOfCurrentCycle() {
        // deploy-engine은 실패 시 파드 로그와 스택트레이스를 한 줄씩 모두 FAILED로 기록한다.
        // 실제 데이터 형태다. "FAILED를 만나면 경계"로 구현하면 마지막 한 줄만 남고
        // 정작 원인이 담긴 줄들이 전부 버려진다(실제로 겪은 버그).
        List<DeploymentLog> logs = logs(
                log(2L, DeploymentStatus.FAILED, "[이전 시도] 포트 오류로 실패"),
                log(3L, DeploymentStatus.PENDING, "배포 요청이 접수되었습니다."),
                log(4L, DeploymentStatus.BUILDING, "산출물 다운로드 완료"),
                log(5L, DeploymentStatus.DEPLOYING, "앱 기동 대기 중"),
                log(6L, DeploymentStatus.FAILED, "제한시간 내에 Ready 상태가 되지 않음:"),
                log(7L, DeploymentStatus.FAILED, "  Caused by: CommunicationsException: Communications link failure"),
                log(8L, DeploymentStatus.FAILED, "  url=jdbc:mysql://localhost:3306/mydb"),
                log(9L, DeploymentStatus.FAILED, "상태 변경: FAILED"));

        FailureLogSlice slice = slicer.slice(logs, 9L, 400);

        // 이번 사이클 전체(PENDING·BUILDING·DEPLOYING + FAILED 4줄) 7줄이 들어와야 한다.
        assertThat(slice.lines()).hasSize(7);
        assertThat(slice.lines()).extracting(DiagnosisLogLine::message)
                .anyMatch(message -> message.contains("CommunicationsException"));
        // 이전 시도 로그는 섞이지 않는다.
        assertThat(slice.lines()).extracting(DiagnosisLogLine::message)
                .noneMatch(message -> message.contains("[이전 시도]"));
    }

    @Test
    @DisplayName("직전 RUNNING 이후부터 잘라낸다 (성공 후 재배포하다 실패한 경우)")
    void slicesAfterPreviousRunning() {
        List<DeploymentLog> logs = logs(
                log(10L, DeploymentStatus.PENDING, "첫 배포 접수"),
                log(20L, DeploymentStatus.RUNNING, "첫 배포 성공"),
                log(30L, DeploymentStatus.PENDING, "환경변수 수정 후 재배포"),
                log(40L, DeploymentStatus.FAILED, "재배포 실패"));

        FailureLogSlice slice = slicer.slice(logs, 40L, 400);

        assertThat(slice.lines()).extracting(DiagnosisLogLine::message)
                .containsExactly("환경변수 수정 후 재배포", "재배포 실패");
    }

    @Test
    @DisplayName("첫 배포라 직전 종료 상태가 없으면 처음부터 포함한다")
    void includesFromBeginningOnFirstCycle() {
        List<DeploymentLog> logs = logs(
                log(1L, DeploymentStatus.PENDING, "접수"),
                log(2L, DeploymentStatus.BUILDING, "빌드"),
                log(3L, DeploymentStatus.FAILED, "실패"));

        FailureLogSlice slice = slicer.slice(logs, 3L, 400);

        assertThat(slice.lines()).hasSize(3);
        assertThat(slice.lines().get(0).message()).isEqualTo("접수");
    }

    @Test
    @DisplayName("줄 번호는 1부터 새로 부여한다")
    void numbersLinesFromOne() {
        List<DeploymentLog> logs = logs(
                log(100L, DeploymentStatus.FAILED, "이전 실패"),
                log(200L, DeploymentStatus.PENDING, "접수"),
                log(300L, DeploymentStatus.FAILED, "실패"));

        FailureLogSlice slice = slicer.slice(logs, 300L, 400);

        assertThat(slice.lines()).extracting(DiagnosisLogLine::lineNumber).containsExactly(1, 2);
    }

    @Test
    @DisplayName("상한을 넘으면 앞부분을 잘라내고 실패 지점에 가까운 뒤쪽을 남긴다")
    void truncatesLeadingLinesWhenOverLimit() {
        List<DeploymentLog> raw = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            raw.add(log((long) i, DeploymentStatus.BUILDING, "빌드 " + i));
        }
        raw.add(log(99L, DeploymentStatus.FAILED, "실패"));

        FailureLogSlice slice = slicer.slice(logs(raw.toArray(new DeploymentLog[0])), 99L, 4);

        assertThat(slice.omittedLeadingLines()).isEqualTo(7);
        assertThat(slice.lines()).extracting(DiagnosisLogLine::message)
                .containsExactly("빌드 8", "빌드 9", "빌드 10", "실패");
        // 잘라낸 뒤에도 번호는 1부터다. AI가 이 번호로 근거를 가리키므로 화면과 기준이 같아야 한다.
        assertThat(slice.lines()).extracting(DiagnosisLogLine::lineNumber).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("잘라낸 줄의 비밀값은 마스킹된다")
    void masksSecretsInSlicedLines() {
        List<DeploymentLog> logs = logs(
                log(1L, DeploymentStatus.BUILDING, "spring.datasource.password=hunter2"),
                log(2L, DeploymentStatus.FAILED, "jdbc:mysql://appuser:s3cret@mysql:3306/db 연결 실패"));

        FailureLogSlice slice = slicer.slice(logs, 2L, 400);

        assertThat(slice.lines()).extracting(DiagnosisLogLine::message)
                .noneMatch(message -> message.contains("hunter2") || message.contains("s3cret"));
    }

    @Test
    @DisplayName("대상 로그 id가 목록에 없으면 빈 구간을 반환한다")
    void returnsEmptyWhenEndLogMissing() {
        List<DeploymentLog> logs = logs(log(1L, DeploymentStatus.FAILED, "실패"));

        FailureLogSlice slice = slicer.slice(logs, 999L, 400);

        assertThat(slice.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("로그가 없으면 빈 구간을 반환한다")
    void returnsEmptyWhenNoLogs() {
        assertThat(slicer.slice(List.of(), 1L, 400).isEmpty()).isTrue();
    }

    private List<DeploymentLog> logs(DeploymentLog... items) {
        return List.of(items);
    }

    private DeploymentLog log(Long id, DeploymentStatus status, String message) {
        DeploymentLog log = new DeploymentLog(null, status, message);
        ReflectionTestUtils.setField(log, "id", id);
        return log;
    }
}
