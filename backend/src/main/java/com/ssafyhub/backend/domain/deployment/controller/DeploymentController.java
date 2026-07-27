package com.ssafyhub.backend.domain.deployment.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateResponse;
import com.ssafyhub.backend.domain.deployment.service.DeploymentService;
import com.ssafyhub.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentCreateResponse>> createDeployment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeploymentCreateRequest request
    ) {
        DeploymentCreateResponse response = deploymentService.createDeployment(userDetails.getMemberId(), request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("배포 요청이 접수되었습니다.", response));
    }
}
