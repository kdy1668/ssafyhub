package com.ssafyhub.backend.domain.deployment.dto;

/**
 * deploy-engine(DevOps WAS)의 GET /api/deployments/{id}/metrics 응답.
 * 값이 아직 없으면(막 떠서 스크레이프 전/미배포) 각 필드가 null일 수 있다.
 */
public record DevOpsMetricsResponse(
        Double cpuCores,
        Long memoryBytes,
        Long networkReceiveBytes,
        Long networkTransmitBytes,
        Long diskUsageBytes,
        Long requestsTotal
) {
}
