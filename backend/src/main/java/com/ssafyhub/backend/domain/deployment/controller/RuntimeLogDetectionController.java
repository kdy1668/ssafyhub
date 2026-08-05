package com.ssafyhub.backend.domain.deployment.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.deployment.dto.RuntimeLogDetectionResponse;
import com.ssafyhub.backend.domain.deployment.service.RuntimeLogDetectionService;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 런타임 로그 이상 감지 API.
 *
 * 기존 DeploymentController에는 GET /{id}/runtime-logs가 있고, 여기서 추가하는 것은
 * POST /{id}/runtime-logs/detection이다. 메서드와 경로가 모두 달라 매핑이 충돌하지 않는다.
 */
@Tag(name = "Runtime Log Detection", description = "런타임 로그 이상 감지 API")
@RestController
@RequestMapping("/api/v1/deployments/{deploymentId}/runtime-logs/detection")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class RuntimeLogDetectionController {

    private final RuntimeLogDetectionService runtimeLogDetectionService;

    @Operation(
            summary = "런타임 로그 이상 감지",
            description = "실행 중인 앱의 최근 로그 스냅샷에서 이상 징후를 찾아 상태(health)와 함께 반환합니다. "
                    + "**사용자가 버튼을 눌렀을 때만 호출해야 하며, 로그 폴링에 연결하면 안 됩니다.** "
                    + "배포당 최소 호출 간격이 있어 그 이내 재요청은 AI_003으로 거부됩니다. "
                    + "간격 값은 GET /api/v1/ai/availability의 runtimeLogCooldownSeconds로 확인하세요. "
                    + "결과는 저장하지 않습니다. 최신 상태가 RUNNING일 때만 가능하며 본인 배포만 조회할 수 있습니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.DEPLOYMENT_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.AI_NOT_ANALYZABLE)
    @ApiErrorCodeExample(ErrorCode.AI_TOO_MANY_REQUESTS)
    @ApiErrorCodeExample(ErrorCode.AI_DISABLED)
    @ApiErrorCodeExample(ErrorCode.AI_CALL_FAILED)
    @PostMapping
    public ResponseEntity<ApiResponse<RuntimeLogDetectionResponse>> detect(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long deploymentId
    ) {
        RuntimeLogDetectionResponse response =
                runtimeLogDetectionService.detect(userDetails.getMemberId(), deploymentId);

        return ResponseEntity.ok(ApiResponse.success("런타임 로그를 확인했습니다.", response));
    }
}
