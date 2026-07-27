package com.ssafyhub.backend.domain.deployment.outbox.service;

import com.ssafyhub.backend.domain.deployment.outbox.dto.OutboxWork;
import com.ssafyhub.backend.domain.deployment.outbox.entity.DeploymentOutbox;
import com.ssafyhub.backend.domain.deployment.outbox.enums.OutboxStatus;
import com.ssafyhub.backend.domain.deployment.outbox.repository.DeploymentOutboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeploymentOutboxStateService {

    private final DeploymentOutboxRepository deploymentOutboxRepository;

    @Transactional
    public List<OutboxWork> claimReadyEvents(LocalDateTime now, int batchSize) {
        List<DeploymentOutbox> outboxes = deploymentOutboxRepository.findReadyForUpdate(
                OutboxStatus.PENDING,
                now,
                PageRequest.of(0, batchSize)
        );

        outboxes.forEach(DeploymentOutbox::startProcessing);

        return outboxes.stream()
                .map(outbox -> new OutboxWork(
                        outbox.getId(),
                        outbox.getDeploymentId(),
                        outbox.getAttemptCount()
                ))
                .toList();
    }

    @Transactional
    public void markPublished(Long outboxId, LocalDateTime now) {
        deploymentOutboxRepository.findById(outboxId)
                .filter(outbox -> outbox.getStatus() == OutboxStatus.PROCESSING)
                .ifPresent(outbox -> outbox.markPublished(now));
    }

    @Transactional
    public void markDeliveryFailed(
            Long outboxId,
            LocalDateTime now,
            String lastError
    ) {
        deploymentOutboxRepository.findById(outboxId)
                .filter(outbox -> outbox.getStatus() == OutboxStatus.PROCESSING)
                .ifPresent(outbox -> outbox.markFailed(now, lastError));
    }

}
