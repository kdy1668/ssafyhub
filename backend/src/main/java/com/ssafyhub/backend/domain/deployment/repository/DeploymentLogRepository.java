package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {

    Optional<DeploymentLog> findFirstByDeployment_IdOrderByIdDesc(Long deploymentId);

    // 배포 진행 로그를 쌓인 순서(오래된→최신)대로 - 깃헙 액션식 단계별 로그 조회용
    List<DeploymentLog> findByDeployment_IdOrderByIdAsc(Long deploymentId);

    void deleteByDeployment_Id(Long deploymentId);
}
