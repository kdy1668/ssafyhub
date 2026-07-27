package com.ssafyhub.backend.domain.deployment.outbox.repository;

import com.ssafyhub.backend.domain.deployment.outbox.entity.DeploymentOutbox;
import com.ssafyhub.backend.domain.deployment.outbox.enums.OutboxStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeploymentOutboxRepository extends JpaRepository<DeploymentOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox
            from DeploymentOutbox outbox
            where outbox.status = :status
              and outbox.nextAttemptAt <= :now
            order by outbox.nextAttemptAt asc, outbox.id asc
            """)
    List<DeploymentOutbox> findReadyForUpdate(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

}
