package com.ssafyhub.backend.domain.deployment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafyhub.backend.global.ai.AiProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuntimeLogCooldownTest {

    private static final Long DEPLOYMENT_ID = 42L;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-05T14:00:00Z"));
    private final AiProperties properties = new AiProperties();
    private final RuntimeLogCooldown cooldown = new RuntimeLogCooldown(properties, clock);

    @Test
    @DisplayName("한 번도 호출하지 않았으면 바로 호출할 수 있다")
    void allowsFirstCall() {
        assertThat(cooldown.remainingSeconds(DEPLOYMENT_ID)).isZero();
    }

    @Test
    @DisplayName("호출 직후에는 설정된 간격만큼 남는다")
    void blocksImmediatelyAfterCall() {
        cooldown.markSuccess(DEPLOYMENT_ID);

        assertThat(cooldown.remainingSeconds(DEPLOYMENT_ID)).isEqualTo(30);
    }

    @Test
    @DisplayName("간격이 지나면 다시 호출할 수 있다")
    void allowsAfterCooldownElapsed() {
        cooldown.markSuccess(DEPLOYMENT_ID);

        clock.advance(Duration.ofSeconds(31));

        assertThat(cooldown.remainingSeconds(DEPLOYMENT_ID)).isZero();
    }

    @Test
    @DisplayName("경과 시간만큼 남은 시간이 줄어든다")
    void decreasesAsTimePasses() {
        cooldown.markSuccess(DEPLOYMENT_ID);

        clock.advance(Duration.ofSeconds(12));

        assertThat(cooldown.remainingSeconds(DEPLOYMENT_ID)).isEqualTo(18);
    }

    @Test
    @DisplayName("배포별로 따로 센다")
    void tracksPerDeployment() {
        cooldown.markSuccess(DEPLOYMENT_ID);

        assertThat(cooldown.remainingSeconds(DEPLOYMENT_ID)).isPositive();
        assertThat(cooldown.remainingSeconds(999L)).isZero();
    }

    @Test
    @DisplayName("설정값을 바꾸면 그 값이 적용된다")
    void honoursConfiguredInterval() {
        properties.getRuntimeLogDetection().setCooldownSeconds(120);
        cooldown.markSuccess(DEPLOYMENT_ID);

        assertThat(cooldown.remainingSeconds(DEPLOYMENT_ID)).isEqualTo(120);
    }

    @Test
    @DisplayName("항목이 상한을 넘으면 오래된 것을 걷어낸다")
    void evictsStaleEntriesOverLimit() {
        // 1시간 이상 지난 항목 1200개를 만든다.
        for (long id = 1; id <= 1200; id++) {
            cooldown.markSuccess(id);
        }
        clock.advance(Duration.ofHours(2));

        // 상한을 넘긴 상태에서 한 번 더 기록하면 정리가 돈다.
        cooldown.markSuccess(9999L);

        assertThat(cooldown.remainingSeconds(1L)).isZero();
        assertThat(cooldown.remainingSeconds(9999L)).isPositive();
    }

    /** 시간을 앞으로 밀 수 있는 테스트용 Clock. */
    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
