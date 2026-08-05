package com.ssafyhub.backend.domain.deployment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 배포 실패 AI 진단 결과. 실패 1건(source_log_id)당 한 행만 존재한다.
 *
 * deployment_id에 FK를 걸지 않는다. FK가 있으면 배포 삭제 시 이 테이블을 먼저 비워야 하고,
 * 그러려면 기존 삭제 로직(DeploymentService.deleteDeployment)을 고쳐야 한다. 진단은 부가
 * 기능이므로 그것 때문에 이미 검증된 삭제 경로가 깨질 위험을 만들지 않는다.
 * 같은 이유로 FK를 두지 않은 deployment_outbox와 같은 방식이다.
 * 남는 고아 행은 OrphanDiagnosisCleaner가 주기적으로 정리한다.
 *
 * payload_json은 진단 본문(요약/설명/조치/근거 줄번호/확신도)을 직렬화해 담는다.
 * 컬럼으로 쪼개지 않는 이유는 조건 검색 대상이 아니고 항상 통째로 읽히기 때문이다.
 * 분석에 사용한 로그 발췌는 저장하지 않는다 - 실패 사이클 로그는 더 이상 바뀌지 않으므로
 * 조회 시점에 같은 규칙으로 다시 잘라내면 동일한 결과가 나온다.
 */
@Getter
@Entity
@Table(
        name = "deployment_diagnoses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deployment_diagnoses_source_log",
                columnNames = {"deployment_id", "source_log_id"}
        ),
        indexes = @Index(
                name = "idx_deployment_diagnoses_latest",
                columnList = "deployment_id, id DESC"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", nullable = false)
    private Long deploymentId;

    /** 진단 대상이 된 FAILED 상태 deployment_logs.id. 캐시 키이자 재배포 구분자다. */
    @Column(name = "source_log_id", nullable = false)
    private Long sourceLogId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(length = 60)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeploymentDiagnosis(
            Long deploymentId,
            Long sourceLogId,
            String payloadJson,
            String model,
            Integer inputTokens,
            Integer outputTokens
    ) {
        this.deploymentId = deploymentId;
        this.sourceLogId = sourceLogId;
        this.payloadJson = payloadJson;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }
}
