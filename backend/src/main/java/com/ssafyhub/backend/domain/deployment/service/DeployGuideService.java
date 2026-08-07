package com.ssafyhub.backend.domain.deployment.service;

import com.ssafyhub.backend.domain.deployment.dto.DeployGuide;
import com.ssafyhub.backend.domain.deployment.dto.DeployGuideRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeployGuideResponse;
import com.ssafyhub.backend.domain.deployment.support.DeployGuidePromptBuilder;
import com.ssafyhub.backend.domain.deployment.support.DeployGuideRenderer;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.ai.AiChatClient;
import com.ssafyhub.backend.global.ai.AiResult;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 온보딩 배포 가이드 생성 (기능 A).
 *
 * 처음 쓰는 사용자가 "통합/분리인지, zip을 어떻게 만드는지, 프론트를 어떻게 백엔드에 연결하는지"를
 * 몰라 막히는 지점을, 프로젝트를 분석해 CLAUDE.md 형식 가이드로 만들어 해소한다.
 *
 * 로그인한 사용자의 학번을 알기 때문에 이 배포의 실제 접속 주소(https://{학번}.도메인)를
 * 확정해 프롬프트와 가이드에 그대로 주입한다 - "어디로 요청해야 하는지"를 추상적 규약이 아니라
 * 구체적 주소로 알려주기 위함이다.
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
    private final MemberRepository memberRepository;

    @Value("${deployment.public-domain}")
    private String publicDomain;

    /**
     * 프로젝트를 분석해 배포 가이드를 생성한다.
     *
     * @throws BusinessException VALIDATION_FAILED - 입력 부족 / MEMBER_NOT_FOUND / AI_DISABLED / AI_CALL_FAILED
     */
    public DeployGuideResponse generate(Long memberId, DeployGuideRequest request) {
        if (request == null || request.fileTree() == null || request.fileTree().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        // 배포되면 이 주소로 접속되고, 프론트가 상대경로 /api로 호출하면 이 주소의 /api로 간다.
        String accessUrl = "https://%s.%s".formatted(member.getStudentNumber(), publicDomain);

        AiResult<DeployGuide> result = aiChatClient.ask(
                DeployGuide.SCHEMA,
                promptBuilder.system(),
                promptBuilder.user(request, accessUrl),
                PURPOSE,
                null);

        String markdown = renderer.render(result.value(), request.projectName(), accessUrl);
        return new DeployGuideResponse(result.value(), markdown, result.model());
    }
}
