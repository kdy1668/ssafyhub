package com.ssafyhub.backend.domain.artifact.controller;

import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlRequest;
import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlResponse;
import com.ssafyhub.backend.domain.artifact.service.ArtifactService;
import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Artifact", description = "배포 산출물 업로드 API")
@RestController
@RequestMapping("/api/v1/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    @Operation(
            summary = "산출물 업로드용 Presigned URL 발급",
            description = "백엔드(.jar)/프론트엔드(.zip) 산출물을 S3에 직접 업로드할 수 있는 Presigned URL을 발급합니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.VALIDATION_FAILED)
    @ApiErrorCodeExample(ErrorCode.MEMBER_NOT_FOUND)
    @ApiErrorCodeExample(ErrorCode.INVALID_BACKEND_FILE)
    @ApiErrorCodeExample(ErrorCode.INVALID_FRONTEND_FILE)
    @ApiErrorCodeExample(ErrorCode.INVALID_FILE_NAME)
    @ApiErrorCodeExample(ErrorCode.PRESIGNED_URL_GENERATION_FAILED)
    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> issuePresignedUrls(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        PresignedUrlResponse response = artifactService.issuePresignedUrls(userDetails.getMemberId(), request);

        return ResponseEntity.ok(ApiResponse.success("업로드 URL이 발급되었습니다.", response));
    }
}
