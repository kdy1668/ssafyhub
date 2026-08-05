package com.ssafyhub.backend.domain.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.deployment.controller.DeploymentDiagnosisController;
import com.ssafyhub.backend.domain.deployment.controller.RuntimeLogDetectionController;
import com.ssafyhub.backend.domain.deployment.outbox.client.DevOpsDeploymentClient;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentDiagnosisRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.deployment.service.DeploymentDiagnosisService;
import com.ssafyhub.backend.domain.deployment.service.OrphanDiagnosisCleaner;
import com.ssafyhub.backend.domain.deployment.service.RuntimeLogDetectionService;
import com.ssafyhub.backend.domain.deployment.support.DiagnosisPromptBuilder;
import com.ssafyhub.backend.domain.deployment.support.FailureLogSlicer;
import com.ssafyhub.backend.domain.deployment.support.RuntimeLogCooldown;
import com.ssafyhub.backend.domain.deployment.support.RuntimeLogPromptBuilder;
import com.ssafyhub.backend.global.ai.AiAvailability;
import com.ssafyhub.backend.global.ai.AiProperties;
import com.ssafyhub.backend.global.ai.ClaudeClient;
import com.ssafyhub.backend.global.ai.SecretRedactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * AI 기능 빈이 토글에 따라 등록/미등록되는지 검증한다.
 *
 * 이 테스트가 필요한 이유: AI 빈들은 ai.enabled=true일 때만 등록되는데, 기존 컨텍스트
 * 테스트는 토글을 켜지 않으므로 "켜진 경로"가 전혀 검증되지 않는다. 그대로 두면 조립이
 * 깨져 있어도 프로덕션에서 기동할 때가 되어서야 알게 된다.
 *
 * 실제로 이 종류의 문제를 두 번 잡았다 - 진단 리포지토리 미주입, RuntimeLogCooldown의
 * 생성자 모호성. 둘 다 컴파일은 통과하고 기동에서만 터진다.
 */
class AiFeatureBeanWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AiFeatureTestConfig.class);

    @Test
    @DisplayName("ai.enabled=true면 두 기능의 서비스·컨트롤러·스케줄러가 모두 등록된다")
    void registersAllAiBeansWhenEnabled() {
        runner.withPropertyValues("ai.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();

            // F1 배포 실패 진단
            assertThat(context).hasSingleBean(DeploymentDiagnosisService.class);
            assertThat(context).hasSingleBean(DeploymentDiagnosisController.class);
            assertThat(context).hasSingleBean(OrphanDiagnosisCleaner.class);

            // F2 런타임 로그 이상 감지
            assertThat(context).hasSingleBean(RuntimeLogDetectionService.class);
            assertThat(context).hasSingleBean(RuntimeLogDetectionController.class);
        });
    }

    @Test
    @DisplayName("토글이 꺼져 있으면 AI 기능 빈이 하나도 등록되지 않는다")
    void registersNothingWhenDisabled() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DeploymentDiagnosisService.class);
            assertThat(context).doesNotHaveBean(DeploymentDiagnosisController.class);
            assertThat(context).doesNotHaveBean(OrphanDiagnosisCleaner.class);
            assertThat(context).doesNotHaveBean(RuntimeLogDetectionService.class);
            assertThat(context).doesNotHaveBean(RuntimeLogDetectionController.class);
        });
    }

    @Test
    @DisplayName("ai.enabled=false를 명시해도 등록되지 않는다")
    void registersNothingWhenExplicitlyFalse() {
        runner.withPropertyValues("ai.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DeploymentDiagnosisService.class);
            assertThat(context).doesNotHaveBean(RuntimeLogDetectionService.class);
        });
    }

    @Test
    @DisplayName("쿨다운 빈은 생성자가 둘이어도 정상 주입된다")
    void wiresCooldownWithTwoConstructors() {
        runner.withPropertyValues("ai.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(RuntimeLogCooldown.class);
            assertThat(context.getBean(RuntimeLogCooldown.class).remainingSeconds(1L)).isZero();
        });
    }

    /**
     * 리포지토리와 외부 클라이언트는 목으로 대체한다. 여기서 확인하려는 것은
     * "조립이 되는가"이지 실제 동작이 아니다.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({
            DeploymentDiagnosisService.class,
            DeploymentDiagnosisController.class,
            OrphanDiagnosisCleaner.class,
            RuntimeLogDetectionService.class,
            RuntimeLogDetectionController.class,
            FailureLogSlicer.class,
            DiagnosisPromptBuilder.class,
            RuntimeLogCooldown.class,
            RuntimeLogPromptBuilder.class,
            SecretRedactor.class
    })
    static class AiFeatureTestConfig {

        @Bean
        DeploymentRepository deploymentRepository() {
            return mock(DeploymentRepository.class);
        }

        @Bean
        DeploymentLogRepository deploymentLogRepository() {
            return mock(DeploymentLogRepository.class);
        }

        @Bean
        DeploymentEnvRepository deploymentEnvRepository() {
            return mock(DeploymentEnvRepository.class);
        }

        @Bean
        DeploymentDiagnosisRepository deploymentDiagnosisRepository() {
            return mock(DeploymentDiagnosisRepository.class);
        }

        @Bean
        DevOpsDeploymentClient devOpsDeploymentClient() {
            return mock(DevOpsDeploymentClient.class);
        }

        @Bean
        ClaudeClient claudeClient() {
            return mock(ClaudeClient.class);
        }

        @Bean
        AiAvailability aiAvailability() {
            return mock(AiAvailability.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
