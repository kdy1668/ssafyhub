package com.ssafyhub.backend.domain.deployment.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ssafyhub.backend.domain.deployment.outbox.entity.DeploymentOutbox;
import com.ssafyhub.backend.domain.deployment.outbox.enums.OutboxStatus;
import com.ssafyhub.backend.domain.deployment.outbox.repository.DeploymentOutboxRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeploymentOutboxStateServiceTest {

    @Mock
    private DeploymentOutboxRepository deploymentOutboxRepository;

    @InjectMocks
    private DeploymentOutboxStateService stateService;

    @Test
    void marksEventFailedAfterDeliveryFailure() {
        DeploymentOutbox outbox = processingOutbox(1);
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 12, 0);
        given(deploymentOutboxRepository.findById(1L)).willReturn(Optional.of(outbox));

        stateService.markDeliveryFailed(
                1L,
                now,
                "connection failed"
        );

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getProcessedAt()).isEqualTo(now);
        assertThat(outbox.getLastError()).isEqualTo("connection failed");
    }

    private DeploymentOutbox processingOutbox(int attemptCount) {
        DeploymentOutbox outbox = DeploymentOutbox.deploymentRequested(100L, LocalDateTime.now());
        ReflectionTestUtils.setField(outbox, "id", 1L);
        ReflectionTestUtils.setField(outbox, "status", OutboxStatus.PROCESSING);
        ReflectionTestUtils.setField(outbox, "attemptCount", attemptCount);
        return outbox;
    }
}
