package com.ssafyhub.backend.domain.showcase.service;

import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentListResponse;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse;
import com.ssafyhub.backend.domain.showcase.dto.ShowcaseResponse;
import com.ssafyhub.backend.domain.showcase.dto.UpdateShowcaseRequest;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowcaseService {

    private final DeploymentRepository deploymentRepository;

    public PublicDeploymentListResponse getPublicDeployments(int page, int size) {
        Page<PublicDeploymentResponse> result =
                deploymentRepository.findPublicDeployments(PageRequest.of(page, size));

        return PublicDeploymentListResponse.from(result);
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
