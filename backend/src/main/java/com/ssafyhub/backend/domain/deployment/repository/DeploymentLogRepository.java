package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {

    Optional<DeploymentLog> findFirstByDeployment_IdOrderByIdDesc(Long deploymentId);

    void deleteByDeployment_Id(Long deploymentId);
}
