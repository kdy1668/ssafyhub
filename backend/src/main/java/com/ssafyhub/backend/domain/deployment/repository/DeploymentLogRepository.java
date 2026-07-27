package com.ssafyhub.backend.domain.deployment.repository;

import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentLogRepository extends JpaRepository<DeploymentLog, Long> {
}
