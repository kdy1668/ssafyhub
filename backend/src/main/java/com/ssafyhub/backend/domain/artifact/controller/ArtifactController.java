package com.ssafyhub.backend.domain.artifact.controller;

import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlRequest;
import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlResponse;
import com.ssafyhub.backend.domain.artifact.service.ArtifactService;
import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> issuePresignedUrls(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        PresignedUrlResponse response = artifactService.issuePresignedUrls(userDetails.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse.success("업로드 URL이 발급되었습니다.", response));
    }
}
