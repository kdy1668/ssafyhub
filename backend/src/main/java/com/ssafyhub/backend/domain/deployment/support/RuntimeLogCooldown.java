package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.global.ai.AiProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 런타임 로그 이상 감지 쿨다운. 배포당 최소 호출 간격을 지킨다.
 *
 * 인메모리로 관리한다. 메인 백엔드는 현재 EC2 1대에 컨테이너 1개로 운영되므로 이 기능을
 * 위해 Redis를 도입하지 않는다. 한계는 명확하다 - 애플리케이션을 재시작하면 쿨다운이
 * 초기화되고, 다중 인스턴스로 확장하면 인스턴스별로 따로 센다. 현재 규모에서 이 한계의
 * 최대 영향은 "재배포 직후 사용자 한 명이 1회 더 호출 가능"이다.
 */
@Component
public class RuntimeLogCooldown {

    /** 맵이 무한히 커지지 않도록 하는 상한. 넘으면 오래된 항목을 걷어낸다. */
    private static final int MAX_ENTRIES = 1000;
    private static final Duration STALE_AFTER = Duration.ofHours(1);

    private final Map<Long, Instant> lastSuccessAt = new ConcurrentHashMap<>();
    private final AiProperties aiProperties;
    private final Clock clock;

    // 생성자가 둘이라 어느 것으로 주입할지 명시해야 한다.
    // @Autowired가 없으면 Spring이 판단하지 못해 기본 생성자를 찾다가 기동에 실패한다.
    @Autowired
    public RuntimeLogCooldown(AiProperties aiProperties) {
        this(aiProperties, Clock.systemUTC());
    }

    /** 테스트에서 시간을 고정하기 위한 생성자. */
    RuntimeLogCooldown(AiProperties aiProperties, Clock clock) {
        this.aiProperties = aiProperties;
        this.clock = clock;
    }

    /** 남은 쿨다운 초. 0이면 지금 호출할 수 있다. */
    public long remainingSeconds(Long deploymentId) {
        Instant last = lastSuccessAt.get(deploymentId);
        if (last == null) {
            return 0;
        }

        long cooldownSeconds = aiProperties.getRuntimeLogDetection().getCooldownSeconds();
        long elapsed = Duration.between(last, clock.instant()).toSeconds();
        return Math.max(0, cooldownSeconds - elapsed);
    }

    /**
     * 호출 성공을 기록한다.
     *
     * 반드시 LLM 호출이 성공한 뒤에만 부른다. 실패에도 기록하면 타임아웃 한 번에
     * 사용자가 쿨다운 내내 재시도조차 못 하게 된다.
     */
    public void markSuccess(Long deploymentId) {
        lastSuccessAt.put(deploymentId, clock.instant());
        if (lastSuccessAt.size() > MAX_ENTRIES) {
            evictStale();
        }
    }

    private void evictStale() {
        Instant threshold = clock.instant().minus(STALE_AFTER);
        Iterator<Map.Entry<Long, Instant>> iterator = lastSuccessAt.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isBefore(threshold)) {
                iterator.remove();
            }
        }
    }
}
