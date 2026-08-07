package com.ssafyhub.backend.domain.deployment.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.deployment.dto.DeployGuideRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeployGuideResponse;
import com.ssafyhub.backend.domain.deployment.service.DeployGuideService;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 온보딩 배포 가이드 생성 API (기능 A).
 *
 * 사용자가 프로젝트(파일 트리 + 핵심 파일)를 보내면, 통합/분리·스택·패키징·API 연결·env를
 * 분석해 CLAUDE.md 형식 가이드를 만들어 준다. 실제 배포 리소스와 무관한 pre-deploy 도구다.
 *
 * ai.enabled=true일 때만 매핑된다(꺼져 있으면 404). 프론트는 GET /api/v1/ai/availability로
 * 먼저 활성 여부를 확인한다.
 */
@Tag(name = "Deploy Guide", description = "온보딩 배포 가이드 생성 API")
@RestController
@RequestMapping("/api/v1/deploy-guide")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class DeployGuideController {

    private final DeployGuideService deployGuideService;

    @Operation(
            summary = "배포 가이드 생성",
            description = "프로젝트의 파일 트리와 핵심 파일을 분석해, SSAFY Hub에 올리는 방법(통합/분리·패키징·"
                    + "프론트→백엔드 연결·필수 환경변수·백킹 서비스)을 구조화 결과와 CLAUDE.md 마크다운으로 반환합니다. "
                    + "비밀값은 서버에서 마스킹 후 분석합니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.VALIDATION_FAILED)
    @ApiErrorCodeExample(ErrorCode.AI_DISABLED)
    @ApiErrorCodeExample(ErrorCode.AI_CALL_FAILED)
    @PostMapping
    public ResponseEntity<ApiResponse<DeployGuideResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeployGuideRequest request
    ) {
        DeployGuideResponse response = deployGuideService.generate(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("배포 가이드를 생성했습니다.", response));
    }
}
