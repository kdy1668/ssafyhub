package com.ssafyhub.backend.domain.showcase.controller;

import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentListResponse;
import com.ssafyhub.backend.domain.showcase.dto.PublicDeploymentResponse;
import com.ssafyhub.backend.domain.showcase.dto.ShowcaseResponse;
import com.ssafyhub.backend.domain.showcase.dto.UpdateShowcaseRequest;
import com.ssafyhub.backend.domain.showcase.service.ShowcaseService;
import com.ssafyhub.backend.global.common.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/showcase")
@RequiredArgsConstructor
public class ShowcaseController {

    private final ShowcaseService showcaseService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicDeploymentListResponse>> getPublicDeployments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PublicDeploymentListResponse response = showcaseService.getPublicDeployments(page, size);

        return ResponseEntity.ok(ApiResponse.success("공개 서비스 목록을 조회했습니다.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicDeploymentResponse>> getPublicDeployment(@PathVariable Long id) {
        PublicDeploymentResponse response = showcaseService.getPublicDeployment(id);

        return ResponseEntity.ok(ApiResponse.success("서비스 상세 정보를 조회했습니다.", response));
    }

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
