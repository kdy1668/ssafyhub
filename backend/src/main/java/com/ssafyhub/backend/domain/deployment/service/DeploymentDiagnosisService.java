package com.ssafyhub.backend.domain.deployment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentDiagnosisResponse;
import com.ssafyhub.backend.domain.deployment.dto.FailureDiagnosis;
import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentDiagnosis;
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
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 배포 실패 AI 진단.
 *
 * 비용 상한이 "실패 1건당 LLM 1회"다. 그 상한은 (deployment_id, source_log_id) 유니크 제약과
 * 캐시 조회로 지켜지며, 프론트가 몇 번 호출하든 LLM 호출 수는 늘지 않는다.
 *
 * 메서드 전체를 @Transactional로 감싸지 않는다. 동시 요청 시 유니크 제약 위반을 잡아
 * "먼저 저장된 진단"을 다시 읽어야 하는데, 하나의 트랜잭션 안에서 제약 위반이 나면 그
 * 트랜잭션은 롤백 표시가 되어 이어지는 조회를 할 수 없다.
 *
 * ai.enabled=true일 때만 빈으로 등록된다. AI를 쓰지 않는 환경에서는 이 기능이 애플리케이션에
 * 아무 흔적도 남기지 않아야 한다 - 진단 전용 리포지토리를 요구하지 않고, 스케줄러도 돌지 않고,
 * 엔드포인트도 매핑되지 않는다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class DeploymentDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentDiagnosisService.class);
    private static final String PURPOSE = "FAILURE_DIAGNOSIS";

    private final DeploymentRepository deploymentRepository;
    private final DeploymentLogRepository deploymentLogRepository;
    private final DeploymentEnvRepository deploymentEnvRepository;
    private final DeploymentDiagnosisRepository diagnosisRepository;
    private final FailureLogSlicer failureLogSlicer;
    private final DiagnosisPromptBuilder promptBuilder;
    private final ClaudeClient claudeClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /**
     * 실패 원인을 진단한다. 같은 실패에 대한 진단이 이미 있으면 그것을 반환하고 AI를 호출하지 않는다.
     *
     * @throws BusinessException DEPLOYMENT_NOT_FOUND / AI_NOT_ANALYZABLE / AI_DISABLED / AI_CALL_FAILED
     */
    public DeploymentDiagnosisResponse diagnose(Long memberId, Long deploymentId) {
        Deployment deployment = getOwnDeployment(memberId, deploymentId);

        DeploymentLog latestLog = deploymentLogRepository
                .findFirstByDeployment_IdOrderByIdDesc(deployment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // 실패하지 않은 배포는 진단할 대상이 없다.
        if (latestLog.getStatus() != DeploymentStatus.FAILED) {
            throw new BusinessException(ErrorCode.AI_NOT_ANALYZABLE);
        }
        Long sourceLogId = latestLog.getId();

        List<DeploymentLog> logsAsc = deploymentLogRepository
                .findByDeployment_IdOrderByIdAsc(deployment.getId());
        FailureLogSlice slice = failureLogSlicer.slice(
                logsAsc, sourceLogId, aiProperties.getFailureDiagnosis().getMaxLogLines());

        // 캐시 조회가 AI 활성 여부 확인보다 앞에 온다.
        // 나중에 기능을 꺼도 이미 만들어진 진단은 계속 볼 수 있어야 한다.
        Optional<DeploymentDiagnosis> cached =
                diagnosisRepository.findByDeploymentIdAndSourceLogId(deployment.getId(), sourceLogId);
        if (cached.isPresent()) {
            return toResponse(cached.get(), slice, true);
        }

        if (slice.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_NOT_ANALYZABLE);
        }

        List<String> envKeys = deploymentEnvRepository
                .findByDeployment_IdOrderByIdAsc(deployment.getId())
                .stream()
                .map(com.ssafyhub.backend.domain.deployment.entity.DeploymentEnv::getEnvKey)
                .toList();

        AiResult<FailureDiagnosis> result = claudeClient.ask(
                FailureDiagnosis.class,
                promptBuilder.system(),
                promptBuilder.user(deployment, envKeys, slice),
                PURPOSE,
                deployment.getId());

        return save(deployment.getId(), sourceLogId, result, slice);
    }

    /** 저장된 최근 진단 1건. AI를 호출하지 않는다. 없으면 empty. */
    public Optional<DeploymentDiagnosisResponse> findLatest(Long memberId, Long deploymentId) {
        Deployment deployment = getOwnDeployment(memberId, deploymentId);

        return diagnosisRepository.findFirstByDeploymentIdOrderByIdDesc(deployment.getId())
                .map(diagnosis -> {
                    List<DeploymentLog> logsAsc = deploymentLogRepository
                            .findByDeployment_IdOrderByIdAsc(deployment.getId());
                    FailureLogSlice slice = failureLogSlicer.slice(
                            logsAsc, diagnosis.getSourceLogId(),
                            aiProperties.getFailureDiagnosis().getMaxLogLines());
                    return toResponse(diagnosis, slice, true);
                });
    }

    private Deployment getOwnDeployment(Long memberId, Long deploymentId) {
        return deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));
    }

    /**
     * 진단을 저장한다.
     *
     * 탭을 두 개 열어두거나 버튼이 중복 클릭되면 같은 (deployment_id, source_log_id)로 동시에
     * 저장이 들어온다. 유니크 제약이 그중 하나를 막는데, 사용자에게는 실패로 보일 이유가 없으므로
     * 먼저 커밋된 진단을 다시 읽어 돌려준다.
     */
    private DeploymentDiagnosisResponse save(
            Long deploymentId, Long sourceLogId, AiResult<FailureDiagnosis> result, FailureLogSlice slice) {
        String payloadJson = serialize(result.value());

        try {
            DeploymentDiagnosis saved = diagnosisRepository.saveAndFlush(new DeploymentDiagnosis(
                    deploymentId,
                    sourceLogId,
                    payloadJson,
                    result.model(),
                    result.inputTokens(),
                    result.outputTokens()));
            return toResponse(saved, slice, false);

        } catch (DataIntegrityViolationException exception) {
            log.info("[AI] 진단 동시 저장 감지. 먼저 저장된 진단을 반환한다. deploymentId={} sourceLogId={}",
                    deploymentId, sourceLogId);
            return diagnosisRepository.findByDeploymentIdAndSourceLogId(deploymentId, sourceLogId)
                    .map(existing -> toResponse(existing, slice, true))
                    // 유니크 위반이 아닌 다른 무결성 오류였다는 뜻이다(예: 진단 중 배포가 삭제됨).
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_CALL_FAILED));
        }
    }

    private DeploymentDiagnosisResponse toResponse(
            DeploymentDiagnosis diagnosis, FailureLogSlice slice, boolean cached) {
        return DeploymentDiagnosisResponse.of(
                diagnosis.getDeploymentId(),
                diagnosis.getSourceLogId(),
                deserialize(diagnosis.getPayloadJson()),
                slice,
                diagnosis.getModel(),
                cached,
                diagnosis.getCreatedAt());
    }

    private String serialize(FailureDiagnosis diagnosis) {
        try {
            return objectMapper.writeValueAsString(diagnosis);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    private FailureDiagnosis deserialize(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, FailureDiagnosis.class);
        } catch (JsonProcessingException exception) {
            // 저장된 진단을 읽을 수 없다면 스키마가 바뀐 것이다. 사용자에게는 일시적 실패로 보인다.
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }
}
