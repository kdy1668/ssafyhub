package com.ssafyhub.backend.global.ai.controller;

import com.ssafyhub.backend.global.ai.AiAvailability;
import com.ssafyhub.backend.global.ai.dto.AiAvailabilityResponse;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI 기능 공통 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAvailabilityController {

    private final AiAvailability aiAvailability;

    @Operation(
            summary = "AI 기능 활성 여부 조회",
            description = "AI 기능(배포 실패 진단 / 런타임 로그 이상 감지)을 사용할 수 있는지 반환합니다. "
                    + "프론트엔드는 이 값이 false면 AI 관련 UI를 렌더링하지 않습니다. "
                    + "서버 설정과 API 키 주입 상태에 따라 결정되며, 기동 후에는 바뀌지 않습니다."
    )
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE_AUTH)
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AiAvailabilityResponse>> getAvailability() {
        AiAvailabilityResponse response = new AiAvailabilityResponse(aiAvailability.isAvailable());

        return ResponseEntity.ok(ApiResponse.success("AI 기능 상태를 조회했습니다.", response));
    }
}
