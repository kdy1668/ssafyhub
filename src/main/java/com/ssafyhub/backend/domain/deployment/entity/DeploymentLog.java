package com.ssafyhub.backend.domain.deployment.entity;

import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(
        name = "deployment_logs",
        indexes = @Index(
                name = "idx_deployment_logs_latest",
                columnList = "deployment_id, id DESC"
        )
)
@Check(constraints = "status IN ('PENDING', 'BUILDING', 'DEPLOYING', 'RUNNING', 'FAILED')")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "deployment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deployment_logs_deployment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Deployment deployment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeploymentStatus status;

    @Column(name = "log_message", nullable = false, columnDefinition = "TEXT")
    private String logMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeploymentLog(Deployment deployment, DeploymentStatus status, String logMessage) {
        this.deployment = deployment;
        this.status = status;
        this.logMessage = logMessage;
    }

}
