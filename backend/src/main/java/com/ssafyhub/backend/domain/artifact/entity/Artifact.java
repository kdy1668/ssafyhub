package com.ssafyhub.backend.domain.artifact.entity;

import com.ssafyhub.backend.domain.artifact.enums.ArtifactStatus;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactType;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "artifacts")
@Check(constraints = "artifact_type IN ('BACKEND_JAR', 'FRONTEND_ZIP', 'SCHEMA_SQL') AND status IN ('URL_ISSUED', 'USED')")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_artifacts_member")
    )
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 30)
    private ArtifactType artifactType;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArtifactStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Artifact(
            Member member,
            ArtifactType artifactType,
            String originalFileName,
            String bucket,
            String objectKey,
            LocalDateTime expiresAt
    ) {
        this.member = member;
        this.artifactType = artifactType;
        this.originalFileName = originalFileName;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.status = ArtifactStatus.URL_ISSUED;
        this.expiresAt = expiresAt;
    }

    public void markUsed() {
        this.status = ArtifactStatus.USED;
    }
}
