package com.ssafyhub.backend.domain.deployment.outbox.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssafyhub.backend.domain.deployment.outbox.client.DevOpsDeploymentClient;
import com.ssafyhub.backend.domain.deployment.outbox.dto.OutboxWork;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeploymentOutboxPublisherTest {

    @Mock
    private DeploymentOutboxStateService stateService;

    @Mock
    private DevOpsDeploymentClient devOpsDeploymentClient;

    @InjectMocks
    private DeploymentOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "batchSize", 20);
    }

    @Test
    void marksEventPublishedWhenDevOpsAcceptsRequest() {
        OutboxWork work = new OutboxWork(1L, 100L, 1);
        given(stateService.claimReadyEvents(any(LocalDateTime.class), eq(20)))
                .willReturn(List.of(work));

        publisher.publishPendingEvents();

        verify(devOpsDeploymentClient).requestDeployment(100L);
        verify(stateService).markPublished(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void marksEventFailedWhenDevOpsRequestFails() {
        OutboxWork work = new OutboxWork(1L, 100L, 2);
        given(stateService.claimReadyEvents(any(LocalDateTime.class), eq(20)))
                .willReturn(List.of(work));
        org.mockito.BDDMockito.willThrow(new IllegalStateException("connection failed"))
                .given(devOpsDeploymentClient)
                .requestDeployment(100L);

        publisher.publishPendingEvents();

        verify(stateService).markDeliveryFailed(
                eq(1L),
                any(LocalDateTime.class),
                eq("IllegalStateException: connection failed")
        );
    }
}
