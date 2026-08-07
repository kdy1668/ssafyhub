package com.ssafyhub.backend.domain.deployment.service;

import com.ssafyhub.backend.domain.deployment.dto.DeployGuide;
import com.ssafyhub.backend.domain.deployment.dto.DeployGuideRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeployGuideResponse;
import com.ssafyhub.backend.domain.deployment.support.DeployGuidePromptBuilder;
import com.ssafyhub.backend.domain.deployment.support.DeployGuideRenderer;
import com.ssafyhub.backend.global.ai.AiChatClient;
import com.ssafyhub.backend.global.ai.AiResult;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 온보딩 배포 가이드 생성 (기능 A).
 *
 * 처음 쓰는 사용자가 "통합/분리인지, zip을 어떻게 만드는지, 프론트를 어떻게 백엔드에 연결하는지"를
 * 몰라 막히는 지점을, 프로젝트를 분석해 CLAUDE.md 형식 가이드로 만들어 해소한다.
 *
 * 배포 실패 진단(DeploymentDiagnosisService)과 달리 특정 배포 리소스에 묶이지 않는 pre-deploy
 * 도구라 캐시/영속화가 없다. 대신 프롬프트 크기를 제한하고 비밀값을 마스킹해 비용·보안을 관리한다.
 *
 * ai.enabled=true일 때만 빈으로 등록된다(AI 미사용 환경엔 흔적을 남기지 않음).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class DeployGuideService {

    private static final String PURPOSE = "DEPLOY_GUIDE";

    private final DeployGuidePromptBuilder promptBuilder;
    private final DeployGuideRenderer renderer;
    private final AiChatClient aiChatClient;

    /**
     * 프로젝트를 분석해 배포 가이드를 생성한다.
     *
     * @throws BusinessException VALIDATION_FAILED - 입력 부족 / AI_DISABLED / AI_CALL_FAILED
     */
    public DeployGuideResponse generate(DeployGuideRequest request) {
        if (request == null || request.fileTree() == null || request.fileTree().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        AiResult<DeployGuide> result = aiChatClient.ask(
                DeployGuide.SCHEMA,
                promptBuilder.system(),
                promptBuilder.user(request),
                PURPOSE,
                null);

        String markdown = renderer.render(result.value(), request.projectName());
        return new DeployGuideResponse(result.value(), markdown, result.model());
    }
}
