package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "배포된 서비스의 리소스 사용량(현재 스냅샷)")
public record ResourceUsageResponse(
        @Schema(description = "배포 ID", example = "11")
        Long id,

        @Schema(description = "CPU 사용률(%) - 코어 사용량 기준", example = "35")
        int cpuPercent,

        @Schema(description = "사용 메모리(MB)", example = "420")
        long memoryUsedMb,

        @Schema(description = "메모리 상한(MB)", example = "512")
        long memoryTotalMb,

        @Schema(description = "샘플 시각")
        LocalDateTime sampledAt
) {
}
