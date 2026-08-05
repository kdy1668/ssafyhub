package com.ssafyhub.backend.domain.deployment.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentDiagnosisResponse;
import com.ssafyhub.backend.domain.deployment.service.DeploymentDiagnosisService;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포 실패 AI 진단 API.
 *
 * 기존 DeploymentController에 메서드를 추가하지 않고 별도 컨트롤러로 분리했다.
 * /{deploymentId}/diagnosis 경로는 기존에 없어 매핑이 충돌하지 않으며,
 * 이미 검증이 끝난 배포 API 파일을 건드리지 않는 것이 안전하다.
 *
 * ai.enabled=true일 때만 매핑된다. 꺼져 있으면 이 경로는 존재하지 않는다(404).
 * 프론트엔드는 GET /api/v1/ai/availability로 먼저 활성 여부를 확인하며,
 * 그 엔드포인트는 토글과 무관하게 항상 응답한다.
 */
@Tag(name = "Deployment Diagnosis", description = "배포 실패 AI 진단 API")
@RestController
@RequestMapping("/api/v1/deployments/{deploymentId}/diagnosis")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class DeploymentDiagnosisController {

    private final DeploymentDiagnosisService deploymentDiagnosisService;

    @Operation(
            summary = "배포 실패 원인 AI 진단",
            description = "실패한 배포의 진행 로그를 분석해 원인·이유·해결 절차를 반환합니다. "
                    + "같은 실패에 대한 진단이 이미 있으면 저장된 결과를 그대로 반환하며(cached=true) "
                    + "AI를 다시 호출하지 않습니다. 따라서 여러 번 호출해도 비용이 늘지 않습니다. "
                    + "최신 상태가 FAILED가 아니면 진단하지 않습니다. 본인 배포만 가능합니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.AI_NOT_ANALYZABLE)
    @ApiErrorCodeExample(ErrorCode.AI_DISABLED)
    @ApiErrorCodeExample(ErrorCode.AI_CALL_FAILED)
    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentDiagnosisResponse>> diagnose(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long deploymentId
    ) {
        DeploymentDiagnosisResponse response =
                deploymentDiagnosisService.diagnose(userDetails.getMemberId(), deploymentId);

        return ResponseEntity.ok(ApiResponse.success("배포 실패 원인을 진단했습니다.", response));
    }

    @Operation(
            summary = "저장된 진단 조회",
            description = "가장 최근에 생성된 진단을 반환합니다. AI를 호출하지 않으며 비용이 발생하지 않습니다. "
                    + "저장된 진단이 없으면 204를 반환합니다. 본인 배포만 조회 가능합니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_NOT_FOUND)
    @GetMapping
    public ResponseEntity<ApiResponse<DeploymentDiagnosisResponse>> getLatest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long deploymentId
    ) {
        Optional<DeploymentDiagnosisResponse> response =
                deploymentDiagnosisService.findLatest(userDetails.getMemberId(), deploymentId);

        return response
                .map(diagnosis -> ResponseEntity.ok(ApiResponse.success("저장된 진단을 조회했습니다.", diagnosis)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
