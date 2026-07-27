package com.ssafyhub.backend.domain.deployment.outbox.repository;

import com.ssafyhub.backend.domain.deployment.outbox.entity.DeploymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentOutboxRepository extends JpaRepository<DeploymentOutbox, Long> {
}
