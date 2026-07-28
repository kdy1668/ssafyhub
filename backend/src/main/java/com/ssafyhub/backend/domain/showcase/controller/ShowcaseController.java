package com.ssafyhub.backend.domain.showcase.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentListResponse;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse;
import com.ssafyhub.backend.domain.showcase.dto.ShowcaseResponse;
import com.ssafyhub.backend.domain.showcase.dto.UpdateShowcaseRequest;
import com.ssafyhub.backend.domain.showcase.service.ShowcaseService;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Showcase", description = "쇼케이스(공개 서비스) API")
@RestController
@RequestMapping("/api/v1/showcase")
@RequiredArgsConstructor
public class ShowcaseController {

    private final ShowcaseService showcaseService;

    @Operation(
            summary = "공개 서비스 목록 조회",
            description = "공개(PUBLIC)로 설정된 배포 목록을 페이지 단위로 조회합니다. 인증이 필요 없습니다."
    )
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicDeploymentListResponse>> getPublicDeployments(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        PublicDeploymentListResponse response = showcaseService.getPublicDeployments(page, size);

        return ResponseEntity.ok(ApiResponse.success("공개 서비스 목록을 조회했습니다.", response));
    }

    @Operation(
            summary = "공개 서비스 상세 조회",
            description = "공개(PUBLIC)로 설정된 배포 1건의 상세 정보를 조회합니다. 인증이 필요 없습니다."
    )
    @ApiErrorCodeExample(ErrorCode.SHOWCASE_NOT_FOUND)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicDeploymentResponse>> getPublicDeployment(@PathVariable Long id) {
        PublicDeploymentResponse response = showcaseService.getPublicDeployment(id);

        return ResponseEntity.ok(ApiResponse.success("서비스 상세 정보를 조회했습니다.", response));
    }

    @Operation(
            summary = "서비스 정보 수정",
            description = "본인이 배포한 서비스의 공개 여부/설명을 수정합니다. 요청에 포함되지 않은(null) 필드는 변경되지 않습니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.SHOWCASE_NOT_FOUND)
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowcaseResponse>> updateShowcase(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody UpdateShowcaseRequest request
    ) {
        ShowcaseResponse response = showcaseService.updateShowcase(userDetails.getMemberId(), id, request);

        return ResponseEntity.ok(ApiResponse.success("서비스 정보를 수정했습니다.", response));
    }
}
