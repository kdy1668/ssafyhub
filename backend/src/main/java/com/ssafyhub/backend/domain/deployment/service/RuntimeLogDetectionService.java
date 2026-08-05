package com.ssafyhub.backend.domain.deployment.service;

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
import com.ssafyhub.backend.global.ai.AiChatClient;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * 런타임 로그 이상 감지.
 *
 * 사용자가 버튼을 눌렀을 때만 실행된다. 런타임 로그 화면은 5초 폴링으로 로그를 갱신하는데,
 * 여기에 감지를 얹으면 화면을 열어두는 것만으로 시간당 720회 LLM 호출이 발생한다.
 * 그래서 폴링과 완전히 분리하고 쿨다운으로 한 번 더 막는다.
 *
 * 결과를 저장하지 않는다. 런타임 로그는 계속 변하므로 캐싱이 무의미하다(F1과의 차이).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class RuntimeLogDetectionService {

    private static final String PURPOSE = "RUNTIME_LOG_DETECTION";

    /** deploy-engine RuntimeLogService가 조회 실패 시 돌려주는 문구의 시작 부분. */
    private static final String FETCH_FAILURE_PREFIX = "(로그 조회 실패:";

    private final DeploymentRepository deploymentRepository;
    private final DeploymentLogRepository deploymentLogRepository;
    private final DevOpsDeploymentClient devOpsDeploymentClient;
    private final RuntimeLogCooldown cooldown;
    private final RuntimeLogPromptBuilder promptBuilder;
    private final AiChatClient aiChatClient;
    private final AiAvailability aiAvailability;
    private final AiProperties aiProperties;
    private final SecretRedactor secretRedactor;

    /**
     * @throws BusinessException DEPLOYMENT_NOT_FOUND / AI_NOT_ANALYZABLE / AI_DISABLED
     *                           / AI_TOO_MANY_REQUESTS / AI_CALL_FAILED
     */
    public RuntimeLogDetectionResponse detect(Long memberId, Long deploymentId) {
        Deployment deployment = deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        DeploymentLog latestLog = deploymentLogRepository
                .findFirstByDeployment_IdOrderByIdDesc(deployment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // 떠 있지 않은 앱은 런타임 로그가 없다.
        if (latestLog.getStatus() != DeploymentStatus.RUNNING) {
            throw new BusinessException(ErrorCode.AI_NOT_ANALYZABLE);
        }

        // 비활성 확인을 쿨다운·로그 조회보다 먼저 한다. 쓸 수 없는 기능 때문에
        // deploy-engine을 호출할 이유가 없다.
        if (!aiAvailability.isAvailable()) {
            throw new BusinessException(ErrorCode.AI_DISABLED);
        }

        if (cooldown.remainingSeconds(deployment.getId()) > 0) {
            throw new BusinessException(ErrorCode.AI_TOO_MANY_REQUESTS);
        }

        String rawLogs = fetchRuntimeLogs(deployment.getId());
        if (isNotAnalyzable(rawLogs)) {
            return RuntimeLogDetectionResponse.unknown(deployment.getId());
        }

        String redactedLogs = secretRedactor.redact(rawLogs);
        int lineCount = countLines(redactedLogs);

        AiResult<RuntimeLogInsight> result = aiChatClient.ask(
                RuntimeLogInsight.SCHEMA,
                promptBuilder.system(),
                promptBuilder.user(deployment, redactedLogs, lineCount),
                PURPOSE,
                deployment.getId());

        // 성공한 뒤에만 쿨다운을 기록한다. 실패에도 기록하면 타임아웃 한 번에
        // 사용자가 30초 동안 재시도조차 못 한다.
        cooldown.markSuccess(deployment.getId());

        return RuntimeLogDetectionResponse.of(
                deployment.getId(), result.value(), lineCount, result.model());
    }

    private String fetchRuntimeLogs(Long deploymentId) {
        try {
            return devOpsDeploymentClient.getRuntimeLogs(
                    deploymentId, aiProperties.getRuntimeLogDetection().getTail());
        } catch (RestClientException exception) {
            // deploy-engine 순단. 사용자에게는 일시적 실패로 보이고 쿨다운은 기록되지 않는다.
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    /**
     * 로그가 비어 있거나 deploy-engine이 조회 실패 문구를 돌려준 경우.
     * 이때는 LLM을 호출하지 않는다 - 분석할 내용이 없는데 비용을 쓸 이유가 없다.
     */
    private boolean isNotAnalyzable(String logs) {
        return logs == null || logs.isBlank() || logs.stripLeading().startsWith(FETCH_FAILURE_PREFIX);
    }

    private int countLines(String logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return (int) logs.lines().count();
    }
}
