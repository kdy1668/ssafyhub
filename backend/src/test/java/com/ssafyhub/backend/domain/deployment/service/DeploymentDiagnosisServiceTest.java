package com.ssafyhub.backend.domain.deployment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentDiagnosisResponse;
import com.ssafyhub.backend.domain.deployment.dto.FailureDiagnosis;
import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentDiagnosis;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentEnv;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentDiagnosisRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.deployment.support.DiagnosisPromptBuilder;
import com.ssafyhub.backend.domain.deployment.support.FailureLogSlice;
import com.ssafyhub.backend.domain.deployment.support.FailureLogSlicer;
import com.ssafyhub.backend.global.ai.AiProperties;
import com.ssafyhub.backend.global.ai.AiResult;
import com.ssafyhub.backend.global.ai.ClaudeClient;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeploymentDiagnosisServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long DEPLOYMENT_ID = 42L;
    private static final Long FAILED_LOG_ID = 1573L;

    @Mock
    private DeploymentRepository deploymentRepository;
    @Mock
    private DeploymentLogRepository deploymentLogRepository;
    @Mock
    private DeploymentEnvRepository deploymentEnvRepository;
    @Mock
    private DeploymentDiagnosisRepository diagnosisRepository;
    @Mock
    private DiagnosisPromptBuilder promptBuilder;
    @Mock
    private ClaudeClient claudeClient;

    private DeploymentDiagnosisService service;
    private Deployment deployment;

    @BeforeEach
    void setUp() {
        service = new DeploymentDiagnosisService(
                deploymentRepository,
                deploymentLogRepository,
                deploymentEnvRepository,
                diagnosisRepository,
                new FailureLogSlicer(new SecretRedactor()),
                promptBuilder,
                claudeClient,
                new AiProperties(),
                new ObjectMapper());

        deployment = mock(Deployment.class);
        given(deployment.getId()).willReturn(DEPLOYMENT_ID);
        given(deploymentRepository.findByIdAndMember_Id(DEPLOYMENT_ID, MEMBER_ID))
                .willReturn(Optional.of(deployment));

        given(promptBuilder.system()).willReturn("system");
        given(promptBuilder.user(any(), any(), any())).willReturn("user");
    }

    @Test
    @DisplayName("실패한 배포를 진단하고 결과를 저장한다")
    void diagnosesFailedDeployment() {
        givenFailedLogs();
        given(diagnosisRepository.findByDeploymentIdAndSourceLogId(DEPLOYMENT_ID, FAILED_LOG_ID))
                .willReturn(Optional.empty());
        given(claudeClient.ask(eq(FailureDiagnosis.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(sampleDiagnosis(), "claude-opus-5", 3128, 612));
        givenSaveReturnsPersisted();

        DeploymentDiagnosisResponse response = service.diagnose(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.cached()).isFalse();
        assertThat(response.sourceLogId()).isEqualTo(FAILED_LOG_ID);
        assertThat(response.summary()).isEqualTo("DB에 연결하지 못했습니다.");
        assertThat(response.actions()).hasSize(1);
        assertThat(response.logLineNumbers()).containsExactly(2);
        assertThat(response.confidence()).isEqualTo(FailureDiagnosis.Confidence.HIGH);
        // 응답만으로 근거를 확인할 수 있어야 한다 - 프론트가 별도 로그 API에 의존하지 않는다.
        assertThat(response.analyzedLog()).isNotEmpty();
        assertThat(response.model()).isEqualTo("claude-opus-5");
    }

    @Test
    @DisplayName("같은 실패에 대한 진단이 있으면 AI를 호출하지 않고 저장된 결과를 반환한다")
    void returnsCachedDiagnosisWithoutCallingAi() {
        givenFailedLogs();
        given(diagnosisRepository.findByDeploymentIdAndSourceLogId(DEPLOYMENT_ID, FAILED_LOG_ID))
                .willReturn(Optional.of(persistedDiagnosis()));

        DeploymentDiagnosisResponse response = service.diagnose(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.cached()).isTrue();
        assertThat(response.summary()).isEqualTo("DB에 연결하지 못했습니다.");
        verifyNoInteractions(claudeClient);
        verify(diagnosisRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("실패 상태가 아니면 진단하지 않는다")
    void rejectsWhenNotFailed() {
        DeploymentLog runningLog = log(1600L, DeploymentStatus.RUNNING, "정상 기동");
        given(deploymentLogRepository.findFirstByDeployment_IdOrderByIdDesc(DEPLOYMENT_ID))
                .willReturn(Optional.of(runningLog));

        assertThatThrownBy(() -> service.diagnose(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_NOT_ANALYZABLE);

        verifyNoInteractions(claudeClient);
    }

    @Test
    @DisplayName("본인 배포가 아니면 조회되지 않는다")
    void rejectsOtherMembersDeployment() {
        given(deploymentRepository.findByIdAndMember_Id(DEPLOYMENT_ID, 999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.diagnose(999L, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DEPLOYMENT_NOT_FOUND);

        verifyNoInteractions(claudeClient);
    }

    @Test
    @DisplayName("환경변수는 키 이름만 프롬프트로 넘어가고 값은 복호화하지 않는다")
    void passesOnlyEnvKeysNeverValues() {
        givenFailedLogs();
        given(diagnosisRepository.findByDeploymentIdAndSourceLogId(DEPLOYMENT_ID, FAILED_LOG_ID))
                .willReturn(Optional.empty());
        given(deploymentEnvRepository.findByDeployment_IdOrderByIdAsc(DEPLOYMENT_ID))
                .willReturn(List.of(
                        new DeploymentEnv(deployment, "JWT_SECRET", "암호화된-값-절대-노출-금지"),
                        new DeploymentEnv(deployment, "DB_HOST", "암호화된-값2")));
        given(claudeClient.ask(eq(FailureDiagnosis.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(sampleDiagnosis(), "claude-opus-5", 10, 10));
        givenSaveReturnsPersisted();

        service.diagnose(MEMBER_ID, DEPLOYMENT_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> envKeysCaptor = ArgumentCaptor.forClass(List.class);
        verify(promptBuilder).user(any(), envKeysCaptor.capture(), any());

        assertThat(envKeysCaptor.getValue()).containsExactly("JWT_SECRET", "DB_HOST");
        assertThat(envKeysCaptor.getValue())
                .noneMatch(key -> key.contains("암호화된-값"));
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약을 위반하면 먼저 저장된 진단을 반환한다")
    void returnsExistingDiagnosisOnConcurrentSave() {
        givenFailedLogs();
        given(diagnosisRepository.findByDeploymentIdAndSourceLogId(DEPLOYMENT_ID, FAILED_LOG_ID))
                .willReturn(Optional.empty(), Optional.of(persistedDiagnosis()));
        given(claudeClient.ask(eq(FailureDiagnosis.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(sampleDiagnosis(), "claude-opus-5", 10, 10));
        given(diagnosisRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("uk_deployment_diagnoses_source_log"));

        DeploymentDiagnosisResponse response = service.diagnose(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response.cached()).isTrue();
        assertThat(response.summary()).isEqualTo("DB에 연결하지 못했습니다.");
    }

    @Test
    @DisplayName("무결성 오류인데 저장된 진단도 없으면 일시적 실패로 처리한다")
    void failsWhenIntegrityErrorAndNoExistingDiagnosis() {
        givenFailedLogs();
        given(diagnosisRepository.findByDeploymentIdAndSourceLogId(DEPLOYMENT_ID, FAILED_LOG_ID))
                .willReturn(Optional.empty());
        given(claudeClient.ask(eq(FailureDiagnosis.class), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(new AiResult<>(sampleDiagnosis(), "claude-opus-5", 10, 10));
        given(diagnosisRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("배포가 삭제됨"));

        assertThatThrownBy(() -> service.diagnose(MEMBER_ID, DEPLOYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_CALL_FAILED);
    }

    @Test
    @DisplayName("저장된 진단 조회는 AI를 호출하지 않는다")
    void findLatestNeverCallsAi() {
        given(diagnosisRepository.findFirstByDeploymentIdOrderByIdDesc(DEPLOYMENT_ID))
                .willReturn(Optional.of(persistedDiagnosis()));
        given(deploymentLogRepository.findByDeployment_IdOrderByIdAsc(DEPLOYMENT_ID))
                .willReturn(failedLogList());

        Optional<DeploymentDiagnosisResponse> response = service.findLatest(MEMBER_ID, DEPLOYMENT_ID);

        assertThat(response).isPresent();
        assertThat(response.get().cached()).isTrue();
        verifyNoInteractions(claudeClient);
    }

    @Test
    @DisplayName("저장된 진단이 없으면 빈 값을 반환한다")
    void findLatestReturnsEmptyWhenNone() {
        given(diagnosisRepository.findFirstByDeploymentIdOrderByIdDesc(DEPLOYMENT_ID))
                .willReturn(Optional.empty());

        assertThat(service.findLatest(MEMBER_ID, DEPLOYMENT_ID)).isEmpty();
        verifyNoInteractions(claudeClient);
    }

    // --- fixtures ---

    private void givenFailedLogs() {
        given(deploymentLogRepository.findFirstByDeployment_IdOrderByIdDesc(DEPLOYMENT_ID))
                .willReturn(Optional.of(log(FAILED_LOG_ID, DeploymentStatus.FAILED, "배포 실패")));
        given(deploymentLogRepository.findByDeployment_IdOrderByIdAsc(DEPLOYMENT_ID))
                .willReturn(failedLogList());
    }

    private List<DeploymentLog> failedLogList() {
        return List.of(
                log(1551L, DeploymentStatus.PENDING, "배포 요청이 접수되었습니다."),
                log(FAILED_LOG_ID, DeploymentStatus.FAILED, "배포 실패"));
    }

    private void givenSaveReturnsPersisted() {
        given(diagnosisRepository.saveAndFlush(any())).willAnswer(invocation -> {
            DeploymentDiagnosis argument = invocation.getArgument(0);
            ReflectionTestUtils.setField(argument, "id", 1L);
            ReflectionTestUtils.setField(argument, "createdAt", LocalDateTime.now());
            return argument;
        });
    }

    private FailureDiagnosis sampleDiagnosis() {
        return new FailureDiagnosis(
                "DB에 연결하지 못했습니다.",
                "DB 주소가 localhost로 되어 있어 앱 자신을 가리켰습니다.",
                List.of(new FailureDiagnosis.Action("DB_HOST를 확인하세요", "환경변수 화면에서 mysql로 바꿔주세요.")),
                List.of(2),
                FailureDiagnosis.Confidence.HIGH);
    }

    private DeploymentDiagnosis persistedDiagnosis() {
        try {
            DeploymentDiagnosis diagnosis = new DeploymentDiagnosis(
                    DEPLOYMENT_ID,
                    FAILED_LOG_ID,
                    new ObjectMapper().writeValueAsString(sampleDiagnosis()),
                    "claude-opus-5",
                    3128,
                    612);
            ReflectionTestUtils.setField(diagnosis, "id", 1L);
            ReflectionTestUtils.setField(diagnosis, "createdAt", LocalDateTime.now());
            return diagnosis;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private DeploymentLog log(Long id, DeploymentStatus status, String message) {
        DeploymentLog log = new DeploymentLog(null, status, message);
        ReflectionTestUtils.setField(log, "id", id);
        return log;
    }
}
