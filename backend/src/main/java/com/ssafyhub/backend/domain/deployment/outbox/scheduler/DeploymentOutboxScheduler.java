package com.ssafyhub.backend.domain.deployment.outbox.scheduler;

import com.ssafyhub.backend.domain.deployment.outbox.service.DeploymentOutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "deployment.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DeploymentOutboxScheduler {

    private final DeploymentOutboxPublisher deploymentOutboxPublisher;

    @Scheduled(fixedDelayString = "${deployment.outbox.polling-delay-ms}")
    public void publishDeploymentEvents() {
        deploymentOutboxPublisher.publishPendingEvents();
    }
}
