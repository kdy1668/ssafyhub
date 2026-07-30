package com.ssafyhub.backend.domain.deployment.dto;

import java.util.List;

/**
 * deploy-engine의 GET /api/deployments/{id}/metrics/range 응답(시계열).
 * 지표별로 (epochSeconds, value) 포인트 목록. 값이 없으면 value가 null.
 */
public record DevOpsMetricsRangeResponse(
        List<Point> cpuCores,
        List<Point> memoryBytes,
        List<Point> networkReceiveBytes,
        List<Point> networkTransmitBytes,
        List<Point> diskUsageBytes,
        List<Point> requestsTotal
) {
    public record Point(long epochSeconds, Double value) {
    }
}
