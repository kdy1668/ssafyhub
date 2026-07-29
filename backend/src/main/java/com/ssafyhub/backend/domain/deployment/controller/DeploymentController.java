package com.ssafyhub.backend.domain.deployment.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateResponse;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentStatusResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(
            summary = "내 배포 상태 조회",
            description = "로그인한 사용자의 현재 배포 상태를 조회합니다. 사용자별 배포는 1개만 존재한다고 가정합니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_NOT_FOUND)
    @GetMapping
    public ResponseEntity<ApiResponse<DeploymentStatusResponse>> getMyDeployment(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DeploymentStatusResponse response = deploymentService.getMyDeployment(userDetails.getMemberId());

        return ResponseEntity.ok(ApiResponse.success("배포 상태를 조회했습니다.", response));
    }

    @Operation(
            summary = "배포 삭제",
            description = "배포를 삭제합니다. 실행 중인 서버(k8s Deployment/Service/Ingress)를 함께 내리고 "
                    + "배포 이력까지 제거하므로 되돌릴 수 없습니다. 본인의 배포만 삭제할 수 있습니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_DELETE_FAILED)
    @DeleteMapping("/{deploymentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeployment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long deploymentId
    ) {
        deploymentService.deleteDeployment(userDetails.getMemberId(), deploymentId);

        return ResponseEntity.ok(ApiResponse.success("배포가 삭제되었습니다.", null));
    }
}
