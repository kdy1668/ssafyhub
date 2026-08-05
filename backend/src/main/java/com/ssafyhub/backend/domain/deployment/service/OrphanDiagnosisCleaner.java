package com.ssafyhub.backend.domain.deployment.service;

import com.ssafyhub.backend.domain.deployment.repository.DeploymentDiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미 삭제된 배포에 딸린 진단 행을 정리한다.
 *
 * deployment_diagnoses는 deployment_id에 FK를 두지 않는다(엔티티 주석 참고). 배포 삭제
 * 경로를 건드리지 않기 위한 선택이며, 그 대가로 삭제된 배포의 진단이 남는다. 행 자체는
 * 작지만 무한히 쌓이게 두지 않기 위해 하루 한 번 정리한다.
 *
 * 트래픽이 없는 새벽에 돌린다. 지울 것이 없으면 DELETE 한 번으로 끝나므로 비용이 없다.
 * ai.enabled=false면 진단이 생기지 않으므로 이 스케줄러도 등록하지 않는다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class OrphanDiagnosisCleaner {

    private static final Logger log = LoggerFactory.getLogger(OrphanDiagnosisCleaner.class);

    private final DeploymentDiagnosisRepository diagnosisRepository;

    @Scheduled(cron = "${ai.failure-diagnosis.orphan-cleanup-cron:0 30 4 * * *}")
    @Transactional
    public void cleanOrphans() {
        try {
            int deleted = diagnosisRepository.deleteOrphans();
            if (deleted > 0) {
                log.info("[AI] 삭제된 배포의 진단 {}건을 정리했다.", deleted);
            }
        } catch (RuntimeException exception) {
            // 정리 실패가 애플리케이션에 영향을 주면 안 된다. 다음 주기에 다시 시도한다.
            log.warn("[AI] 진단 정리에 실패했다. 다음 주기에 재시도한다: {}", exception.getMessage());
        }
    }
}
