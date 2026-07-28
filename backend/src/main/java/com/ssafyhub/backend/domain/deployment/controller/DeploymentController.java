package com.ssafyhub.backend.domain.deployment.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateResponse;
import com.ssafyhub.backend.domain.deployment.service.DeploymentService;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Deployment", description = "배포 API")
@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @Operation(
            summary = "배포 생성",
            description = "사전에 발급받은 Artifact를 사용해 배포를 요청합니다. "
                    + "배포는 비동기로 처리되며, 사용자당 하나의 배포만 존재할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "배포 요청 접수 성공")
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.VALIDATION_FAILED)
    @ApiErrorCodeExample(ErrorCode.MEMBER_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_ALREADY_EXISTS)
    @ApiErrorCodeExample(ErrorCode.UNSUPPORTED_RUNTIME_VERSION)
    @ApiErrorCodeExample(ErrorCode.DUPLICATE_ENV_KEY)
    @ApiErrorCodeExample(ErrorCode.ARTIFACT_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.ARTIFACT_ACCESS_DENIED)
    @ApiErrorCodeExample(ErrorCode.ARTIFACT_TYPE_MISMATCH)
    @ApiErrorCodeExample(ErrorCode.ARTIFACT_ALREADY_USED)
    @ApiErrorCodeExample(ErrorCode.ARTIFACT_FILE_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.ARTIFACT_STORAGE_CHECK_FAILED)
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
