package com.ssafyhub.backend.domain.deployment.outbox.service;

import com.ssafyhub.backend.domain.deployment.outbox.client.DevOpsDeploymentClient;
import com.ssafyhub.backend.domain.deployment.outbox.dto.OutboxWork;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentOutboxPublisher {

    private static final int MAX_ERROR_LENGTH = 500;

    private final DeploymentOutboxStateService stateService;
    private final DevOpsDeploymentClient devOpsDeploymentClient;

    @Value("${deployment.outbox.batch-size}")
    private int batchSize;

    public void publishPendingEvents() {
        List<OutboxWork> works = stateService.claimReadyEvents(LocalDateTime.now(), batchSize);
        works.forEach(this::publish);
    }

    private void publish(OutboxWork work) {
        try {
            devOpsDeploymentClient.requestDeployment(work.deploymentId());
            stateService.markPublished(work.outboxId(), LocalDateTime.now());
            log.info(
                    "Published deployment outbox event: outboxId={}, deploymentId={}, attempt={}",
                    work.outboxId(),
                    work.deploymentId(),
                    work.attemptCount()
            );
        } catch (Exception exception) {
            String error = summarize(exception);
            stateService.markDeliveryFailed(
                    work.outboxId(),
                    LocalDateTime.now(),
                    error
            );
            log.warn(
                    "Failed to publish deployment outbox event: outboxId={}, deploymentId={}, attempt={}, errorType={}",
                    work.outboxId(),
                    work.deploymentId(),
                    work.attemptCount(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private String summarize(Exception exception) {
        String message = exception.getMessage();
        String summary = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return summary.substring(0, Math.min(summary.length(), MAX_ERROR_LENGTH));
    }
}
