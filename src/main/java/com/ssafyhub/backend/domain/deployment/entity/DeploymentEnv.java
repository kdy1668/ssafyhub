package com.ssafyhub.backend.domain.deployment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Table(
        name = "deployment_envs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deployment_envs_key",
                columnNames = {"deployment_id", "env_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentEnv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "deployment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deployment_envs_deployment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Deployment deployment;

    @Column(name = "env_key", nullable = false, length = 100)
    private String envKey;

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeploymentEnv(Deployment deployment, String envKey, String encryptedValue) {
        this.deployment = deployment;
        this.envKey = envKey;
        this.encryptedValue = encryptedValue;
    }
}
