package com.ssafyhub.backend.domain.deployment.outbox.dto;

public record OutboxWork(
        Long outboxId,
        Long deploymentId,
        int attemptCount
) {
}
