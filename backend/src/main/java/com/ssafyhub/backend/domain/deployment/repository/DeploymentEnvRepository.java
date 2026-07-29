package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentEnv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentEnvRepository extends JpaRepository<DeploymentEnv, Long> {

    void deleteByDeployment_Id(Long deploymentId);
}
