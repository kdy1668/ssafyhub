package com.ssafyhub.backend.domain.deployment.outbox.entity;

import com.ssafyhub.backend.domain.deployment.outbox.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "deployment_outbox",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deployment_outbox_event",
                columnNames = {"deployment_id", "event_type"}
        ),
        indexes = @Index(
                name = "idx_deployment_outbox_pending",
                columnList = "status, next_attempt_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentOutbox {

    public static final String DEPLOYMENT_REQUESTED = "DEPLOYMENT_REQUESTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", nullable = false)
    private Long deploymentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private DeploymentOutbox(Long deploymentId, String payload, LocalDateTime nextAttemptAt) {
        this.deploymentId = deploymentId;
        this.eventType = DEPLOYMENT_REQUESTED;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = nextAttemptAt;
    }

    public static DeploymentOutbox deploymentRequested(Long deploymentId, LocalDateTime now) {
        return new DeploymentOutbox(
                deploymentId,
                "{\"deploymentId\":%d}".formatted(deploymentId),
                now
        );
    }
}
