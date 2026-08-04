package com.ssafyhub.backend.domain.deployment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "배포 리소스 사용량 시계열(그래프용)")
public record ResourceHistoryResponse(
        @Schema(description = "CPU 사용률(%) 시계열")
        List<SeriesPoint> cpu,

        @Schema(description = "메모리 사용량(MB) 시계열")
        List<SeriesPoint> memory,

        @Schema(description = "네트워크 누적 수신량(MB) 시계열")
        List<SeriesPoint> networkReceive,

        @Schema(description = "네트워크 누적 송신량(MB) 시계열")
        List<SeriesPoint> networkTransmit,

        @Schema(description = "누적 요청 수 시계열")
        List<SeriesPoint> requests
) {
    @Schema(description = "시계열 포인트")
    public record SeriesPoint(
            @Schema(description = "유닉스 시각(초)", example = "1785385362")
            long time,
            @Schema(description = "값 (CPU %, 또는 메모리 MB)", example = "35.2")
            double value
    ) {
    }
}
