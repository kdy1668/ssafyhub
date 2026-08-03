package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentEnv;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentEnvRepository extends JpaRepository<DeploymentEnv, Long> {

    void deleteByDeployment_Id(Long deploymentId);

    List<DeploymentEnv> findByDeployment_IdOrderByIdAsc(Long deploymentId);
}
