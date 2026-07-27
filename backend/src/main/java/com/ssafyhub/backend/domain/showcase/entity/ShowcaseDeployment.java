package com.ssafyhub.backend.domain.showcase.entity;

import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.showcase.enums.Visibility;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "deployments")
@Check(constraints = "visibility IN ('PUBLIC', 'PRIVATE')")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShowcaseDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deployments_member")
    )
    private Member member;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PRIVATE'")
    private Visibility visibility;

    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changeVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void changeDescription(String description) {
        this.description = description;
    }
}
