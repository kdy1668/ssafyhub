package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentDiagnosis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DeploymentDiagnosisRepository extends JpaRepository<DeploymentDiagnosis, Long> {

    /** 진단 캐시 조회. 같은 실패(source_log_id)에 대해 LLM을 다시 호출하지 않기 위한 키. */
    Optional<DeploymentDiagnosis> findByDeploymentIdAndSourceLogId(Long deploymentId, Long sourceLogId);

    /** 해당 배포의 가장 최근 진단 1건. */
    Optional<DeploymentDiagnosis> findFirstByDeploymentIdOrderByIdDesc(Long deploymentId);

    /**
     * 이미 삭제된 배포에 딸린 고아 진단을 지운다.
     * deployment_id에 FK를 두지 않았으므로(엔티티 주석 참고) 배포 삭제 시 자동으로 지워지지 않는다.
     */
    @Modifying
    @Query("delete from DeploymentDiagnosis d "
            + "where d.deploymentId not in (select p.id from Deployment p)")
    int deleteOrphans();
}
