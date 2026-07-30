package com.ssafyhub.backend.domain.deployment.entity;

import com.ssafyhub.backend.domain.artifact.entity.Artifact;
import com.ssafyhub.backend.domain.deployment.enums.Visibility;
import com.ssafyhub.backend.domain.member.entity.Member;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "deployments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deployments_member_id", columnNames = "member_id"),
                @UniqueConstraint(name = "uk_deployments_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_deployments_backend_artifact_id", columnList = "backend_artifact_id"),
                @Index(name = "idx_deployments_frontend_artifact_id", columnList = "frontend_artifact_id")
        }
)
@Check(constraints = "internal_port BETWEEN 1 AND 65535 AND visibility IN ('PUBLIC', 'PRIVATE')")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deployments_member")
    )
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "backend_artifact_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deployments_backend_artifact")
    )
    private Artifact backendArtifact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "frontend_artifact_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deployments_frontend_artifact")
    )
    private Artifact frontendArtifact;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(nullable = false, length = 150)
    private String slug;

    @Column(name = "runtime_version", nullable = false, length = 20)
    private String runtimeVersion;

    @Column(name = "internal_port", nullable = false)
    private int internalPort;

    // DB가 필요 없는 앱(SQLite 내장형 등)도 있어서 nullable - null이면 "DB 프로비저닝 안 함"을 뜻한다.
    @Column(name = "database_name", length = 100)
    private String databaseName;

    @Column(name = "encrypted_database_password", columnDefinition = "TEXT")
    private String encryptedDatabasePassword;

    // MySQL 외에 추가로 필요한 백킹서비스, 콤마 구분 문자열(예: "REDIS"). 없으면 null.
    @Column(name = "needed_services", length = 200)
    private String neededServices;

    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PRIVATE'")
    private Visibility visibility;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Deployment(
            Member member,
            Artifact backendArtifact,
            Artifact frontendArtifact,
            String projectName,
            String slug,
            String runtimeVersion,
            int internalPort,
            String databaseName,
            String encryptedDatabasePassword,
            String neededServices,
            LocalDateTime requestedAt,
            Visibility visibility,
            String description
    ) {
        this.member = member;
        this.backendArtifact = backendArtifact;
        this.frontendArtifact = frontendArtifact;
        this.projectName = projectName;
        this.slug = slug;
        this.runtimeVersion = runtimeVersion;
        this.internalPort = internalPort;
        this.databaseName = databaseName;
        this.encryptedDatabasePassword = encryptedDatabasePassword;
        this.neededServices = neededServices;
        this.requestedAt = requestedAt;
        this.visibility = (visibility != null) ? visibility : Visibility.PRIVATE;
        this.description = description;
    }

    public void changeVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void changeDescription(String description) {
        this.description = description;
    }
}
