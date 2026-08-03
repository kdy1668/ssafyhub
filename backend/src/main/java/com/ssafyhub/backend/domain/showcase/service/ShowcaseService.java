package com.ssafyhub.backend.domain.showcase.service;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentListResponse;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse;
import com.ssafyhub.backend.domain.showcase.dto.ShowcaseResponse;
import com.ssafyhub.backend.domain.showcase.dto.UpdateShowcaseRequest;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowcaseService {

    private final DeploymentRepository deploymentRepository;

    @Value("${deployment.public-domain}")
    private String publicDomain;

    public PublicDeploymentListResponse getPublicDeployments(int page, int size) {
        Page<PublicDeploymentResponse> result = deploymentRepository
                .findByVisibilityOrderByUpdatedAtDesc(Visibility.PUBLIC, PageRequest.of(page, size))
                .map(this::toPublicDeploymentResponse);

        return PublicDeploymentListResponse.from(result);
    }

    public PublicDeploymentResponse getPublicDeployment(Long deploymentId) {
        return deploymentRepository.findByIdAndVisibility(deploymentId, Visibility.PUBLIC)
                .map(this::toPublicDeploymentResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWCASE_NOT_FOUND));
    }

    // endpoint_url(내부 프록시 타깃 IP)을 그대로 노출하면 방문자가 내부 주소로 직접 들어가
    // 오류를 겪는다(실제로 겪은 문제) - 항상 공개 서브도메인 주소를 만들어 채운다.
    private PublicDeploymentResponse toPublicDeploymentResponse(Deployment deployment) {
        return new PublicDeploymentResponse(
                deployment.getId(),
                deployment.getMember().getTeamName(),
                deployment.getProjectName(),
                deployment.getDescription(),
                "https://%s.%s".formatted(deployment.getSlug(), publicDomain),
                deployment.getUpdatedAt()
        );
    }

    @Transactional
    public ShowcaseResponse updateShowcase(Long memberId, Long deploymentId, UpdateShowcaseRequest request) {
        Deployment deployment = deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWCASE_NOT_FOUND));

        if (request.visibility() != null) {
            deployment.changeVisibility(request.visibility());
        }

        if (request.description() != null) {
            deployment.changeDescription(request.description());
        }

        return ShowcaseResponse.from(deployment);
    }
}
