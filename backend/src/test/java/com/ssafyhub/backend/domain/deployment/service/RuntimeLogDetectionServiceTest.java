package com.ssafyhub.backend.domain.deployment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ssafyhub.backend.domain.deployment.dto.RuntimeLogDetectionResponse;
import com.ssafyhub.backend.domain.deployment.dto.RuntimeLogInsight;
import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.domain.deployment.outbox.client.DevOpsDeploymentClient;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.deployment.support.RuntimeLogCooldown;
import com.ssafyhub.backend.domain.deployment.support.RuntimeLogPromptBuilder;
import com.ssafyhub.backend.global.ai.AiAvailability;
import com.ssafyhub.backend.global.ai.AiProperties;
import com.ssafyhub.backend.global.ai.AiResult;
import com.ssafyhub.backend.global.ai.ClaudeClient;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuntimeLogDetectionServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long DEPLOYMENT_ID = 42L;

    @Mock
    private DeploymentRepository deploymentRepository;
    @Mock
    private DeploymentLogRepository deploymentLogRepository;
    @Mock
    private DevOpsDeploymentClient devOpsDeploymentClient;
    @Mock
    private RuntimeLogCooldown cooldown;
    @Mock
    private ClaudeClient claudeClient;
    @Mock
    private AiAvailability aiAvailability;

    private RuntimeLogDetectionService service;
    private Deployment deployment;

    @BeforeEach
    void setUp() {
        service = new RuntimeLogDetectionService(
                deploymentRepository,
                deploymentLogRepository,
                devOpsDeploymentClient,
                cooldown,
                new RuntimeLogPromptBuilder(),
                claudeClient,
                aiAvailability,
                new AiProperties(),
                new SecretRedactor());

        deployment = mock(Deployment.class);
        given(deployment.getId()).willReturn(DEPLOYMENT_ID);
        given(deployment.getEffectiveTechStack()).willReturn("SPRING_BOOT");
        given(deploymentRepository.findByIdAndMember_Id(DEPLOYMENT_ID, MEMBER_ID))
                .willReturn(Optional.of(deployment));
        given(aiAvailability.isAvailable()).willReturn(true);
        given(cooldown.remainingSeconds(DEPLOYMENT_ID)).willReturn(0L);
        givenRunning();
    }

    @Test
    @DisplayName("실행 중인 배포의 로그를 분석하고 쿨다운을 기록한다")
    void detectsAnomaliesAndMarksCooldown() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt()))
                .willReturn("INFO Started Application\nERROR connection timeout\n");
        given(claudeClient.ask(eq(RuntimeLogInsight.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(warningInsight(), "claude-opus-5", 900, 300));

        RuntimeLogDetectionResponse response = service.detect(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.health())
                .isEqualTo(RuntimeLogDetectionResponse.DetectionHealth.WARNING);
        assertThat(response.findings()).hasSize(1);
        assertThat(response.analyzedLines()).isEqualTo(2);
        assertThat(response.model()).isEqualTo("claude-opus-5");
        verify(cooldown).markSuccess(DEPLOYMENT_ID);
    }

    @Test
    @DisplayName("문제가 없으면 HEALTHY와 빈 findings를 그대로 반환한다")
    void reportsHealthyWithoutInventingProblems() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt()))
                .willReturn("INFO Started Application in 6.2 seconds\n");
        given(claudeClient.ask(eq(RuntimeLogInsight.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(new RuntimeLogInsight(
                        RuntimeLogInsight.Health.HEALTHY,
                        "특별한 문제가 보이지 않습니다.",
                        List.of(),
                        List.of()), "claude-opus-5", 100, 50));

        RuntimeLogDetectionResponse response = service.detect(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.health())
                .isEqualTo(RuntimeLogDetectionResponse.DetectionHealth.HEALTHY);
        assertThat(response.findings()).isEmpty();
    }

    @Test
    @DisplayName("실행 중이 아니면 감지하지 않는다")
    void rejectsWhenNotRunning() {
        given(deploymentLogRepository.findFirstByDeployment_IdOrderByIdDesc(DEPLOYMENT_ID))
                .willReturn(Optional.of(log(DeploymentStatus.FAILED, "배포 실패")));

        assertThatThrownBy(() -> service.detect(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_NOT_ANALYZABLE);

        verifyNoInteractions(devOpsDeploymentClient, claudeClient);
    }

    @Test
    @DisplayName("본인 배포가 아니면 조회되지 않는다")
    void rejectsOtherMembersDeployment() {
        given(deploymentRepository.findByIdAndMember_Id(DEPLOYMENT_ID, 999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.detect(999L, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DEPLOYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("AI가 비활성이면 deploy-engine을 호출하지 않는다")
    void doesNotCallDeployEngineWhenAiDisabled() {
        given(aiAvailability.isAvailable()).willReturn(false);

        assertThatThrownBy(() -> service.detect(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_DISABLED);

        verifyNoInteractions(devOpsDeploymentClient, claudeClient);
    }

    @Test
    @DisplayName("쿨다운 중이면 로그도 가져오지 않고 거부한다")
    void rejectsDuringCooldownWithoutFetchingLogs() {
        given(cooldown.remainingSeconds(DEPLOYMENT_ID)).willReturn(18L);

        assertThatThrownBy(() -> service.detect(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_TOO_MANY_REQUESTS);

        verifyNoInteractions(devOpsDeploymentClient, claudeClient);
    }

    @Test
    @DisplayName("로그가 비어 있으면 UNKNOWN을 반환하고 AI를 호출하지 않는다")
    void returnsUnknownWhenLogsBlank() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt())).willReturn("   \n");

        RuntimeLogDetectionResponse response = service.detect(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.health())
                .isEqualTo(RuntimeLogDetectionResponse.DetectionHealth.UNKNOWN);
        assertThat(response.model()).isNull();
        verifyNoInteractions(claudeClient);
        verify(cooldown, never()).markSuccess(anyLong());
    }

    @Test
    @DisplayName("deploy-engine이 조회 실패 문구를 돌려주면 UNKNOWN을 반환하고 AI를 호출하지 않는다")
    void returnsUnknownWhenFetchFailureMessage() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt()))
                .willReturn("(로그 조회 실패: [IOException] connection refused)");

        RuntimeLogDetectionResponse response = service.detect(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.health())
                .isEqualTo(RuntimeLogDetectionResponse.DetectionHealth.UNKNOWN);
        verifyNoInteractions(claudeClient);
    }

    @Test
    @DisplayName("deploy-engine 순단은 일시적 실패로 처리한다")
    void failsWhenDeployEngineUnreachable() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt()))
                .willThrow(new RestClientException("connection reset"));

        assertThatThrownBy(() -> service.detect(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_CALL_FAILED);

        verify(cooldown, never()).markSuccess(anyLong());
    }

    @Test
    @DisplayName("AI 호출이 실패하면 쿨다운을 기록하지 않아 즉시 재시도할 수 있다")
    void doesNotMarkCooldownWhenAiFails() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt()))
                .willReturn("ERROR something\n");
        given(claudeClient.ask(eq(RuntimeLogInsight.class), anyString(), anyString(), anyString(), anyLong()))
                .willThrow(new BusinessException(ErrorCode.AI_CALL_FAILED));

        assertThatThrownBy(() -> service.detect(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class);

        verify(cooldown, never()).markSuccess(anyLong());
    }

    @Test
    @DisplayName("로그의 비밀값은 마스킹된 뒤 프롬프트에 들어간다")
    void masksSecretsBeforeSendingToAi() {
        given(devOpsDeploymentClient.getRuntimeLogs(eq(DEPLOYMENT_ID), anyInt()))
                .willReturn("DB_PASSWORD=hunter2 로 접속 시도\nAuthorization: Bearer abcdef123456\n");
        given(claudeClient.ask(eq(RuntimeLogInsight.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(warningInsight(), "claude-opus-5", 10, 10));

        service.detect(MEMBER_ID, DEPLOYMENT_ID);

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(claudeClient).ask(any(), anyString(), userPrompt.capture(), anyString(), anyLong());

        assertThat(userPrompt.getValue())
                .doesNotContain("hunter2")
                .doesNotContain("abcdef123456")
                .contains("***");
    }

    // --- fixtures ---

    private void givenRunning() {
        given(deploymentLogRepository.findFirstByDeployment_IdOrderByIdDesc(DEPLOYMENT_ID))
                .willReturn(Optional.of(log(DeploymentStatus.RUNNING, "정상 기동")));
    }

    private RuntimeLogInsight warningInsight() {
        return new RuntimeLogInsight(
                RuntimeLogInsight.Health.WARNING,
                "같은 오류가 반복되고 있습니다.",
                List.of(new RuntimeLogInsight.Finding(
                        RuntimeLogInsight.Severity.WARNING,
                        "DB 커넥션 획득 시간 초과가 반복됩니다",
                        "최근 300줄 중 12번 발생했습니다.",
                        "커넥션을 닫지 않는 코드가 있는지 확인해 보세요.")),
                List.of("리소스 모니터링에서 메모리 사용량을 확인해 보세요."));
    }

    private DeploymentLog log(DeploymentStatus status, String message) {
        DeploymentLog log = new DeploymentLog(null, status, message);
        ReflectionTestUtils.setField(log, "id", 100L);
        return log;
    }
}
