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

    // 프론트만 있는 배포(백엔드 없이 정적 사이트만)도 있어서 nullable - null이면 "백엔드 없음"을 뜻한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "backend_artifact_id",
            foreignKey = @ForeignKey(name = "fk_deployments_backend_artifact")
    )
    private Artifact backendArtifact;

    // 백엔드가 프론트를 직접 서빙하는 모놀리식 구조(예: Spring static 리소스, express.static)도
    // 있어서 nullable - null이면 "별도 프론트 배포 없음(SubdomainProxyFilter가 전부 백엔드로 전달)"을 뜻한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "frontend_artifact_id",
            foreignKey = @ForeignKey(name = "fk_deployments_frontend_artifact")
    )
    private Artifact frontendArtifact;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(nullable = false, length = 150)
    private String slug;

    // 백엔드가 없으면(프론트만) 의미가 없어서 nullable.
    @Column(name = "runtime_version", length = 20)
    private String runtimeVersion;

    // 언어+프레임워크 조합("SPRING_BOOT"/"DJANGO"/"FASTAPI"/"FLASK"/"EXPRESS"). 플랫폼이 이 값에 맞는
    // Dockerfile을 자동 생성하므로 사용자는 Dockerfile을 올릴 필요가 없다. 과거 데이터는 SPRING_BOOT로 간주.
    @Column(name = "tech_stack", length = 30)
    private String techStack;

    // 백엔드가 없으면(프론트만) 의미가 없어서 nullable.
    @Column(name = "internal_port")
    private Integer internalPort;

    // DB가 필요 없는 앱(SQLite 내장형 등)도 있어서 nullable - null이면 "DB 프로비저닝 안 함"을 뜻한다.
    @Column(name = "database_name", length = 100)
    private String databaseName;

    @Column(name = "encrypted_database_password", columnDefinition = "TEXT")
    private String encryptedDatabasePassword;

    // 관계형 DB 엔진(현재 "MYSQL"만 지원). databaseName이 있는데 이 값이 없는 과거 데이터는
    // "MYSQL"로 간주한다(getEffectiveDatabaseEngine 참고).
    @Column(name = "database_engine", length = 20)
    private String databaseEngine;

    // DB 초기화(테이블 생성) SQL - MyBatis 등 자동 생성이 안 되는 프레임워크를 쓸 때만 선택 업로드.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "schema_artifact_id",
            foreignKey = @ForeignKey(name = "fk_deployments_schema_artifact")
    )
    private Artifact schemaArtifact;

    // MySQL/PostgreSQL 외에 추가로 필요한 백킹서비스, 콤마 구분 문자열(예: "REDIS,MONGODB"). 없으면 null.
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
            String techStack,
            Integer internalPort,
            String databaseName,
            String encryptedDatabasePassword,
            String databaseEngine,
            Artifact schemaArtifact,
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
        this.techStack = techStack;
        this.internalPort = internalPort;
        this.databaseName = databaseName;
        this.encryptedDatabasePassword = encryptedDatabasePassword;
        this.databaseEngine = databaseEngine;
        this.schemaArtifact = schemaArtifact;
        this.neededServices = neededServices;
        this.requestedAt = requestedAt;
        this.visibility = (visibility != null) ? visibility : Visibility.PRIVATE;
        this.description = description;
    }

    /** 과거 데이터(techStack 컬럼이 없던 시절, .jar만 지원)는 SPRING_BOOT로 간주한다. */
    public String getEffectiveTechStack() {
        return (techStack == null || techStack.isBlank()) ? "SPRING_BOOT" : techStack;
    }

    /** databaseName이 있는데 엔진이 비어 있는 과거 데이터는 MYSQL로 간주한다. */
    public String getEffectiveDatabaseEngine() {
        if (databaseName == null || databaseName.isBlank()) {
            return null;
        }
        return (databaseEngine == null || databaseEngine.isBlank()) ? "MYSQL" : databaseEngine;
    }

    public void changeVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void changeDescription(String description) {
        this.description = description;
    }
}
