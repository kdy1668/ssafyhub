package com.ssafyhub.backend.domain.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.deployment.controller.DeploymentDiagnosisController;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentDiagnosisRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.deployment.service.DeploymentDiagnosisService;
import com.ssafyhub.backend.domain.deployment.service.OrphanDiagnosisCleaner;
import com.ssafyhub.backend.domain.deployment.support.DiagnosisPromptBuilder;
import com.ssafyhub.backend.domain.deployment.support.FailureLogSlicer;
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
 * 진단 기능 빈이 토글에 따라 실제로 등록/미등록되는지 검증한다.
 *
 * 이 테스트가 필요한 이유: 진단 빈들은 ai.enabled=true일 때만 등록되는데, 기존 컨텍스트
 * 테스트는 토글을 켜지 않으므로 "켜진 경로"가 전혀 검증되지 않는다. 그대로 두면 조립이
 * 깨져 있어도 프로덕션에서 기동할 때가 되어서야 알게 된다.
 */
class DiagnosisBeanWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DiagnosisTestConfig.class);

    @Test
    @DisplayName("ai.enabled=true면 진단 서비스·컨트롤러·정리 스케줄러가 모두 등록된다")
    void registersDiagnosisBeansWhenEnabled() {
        runner.withPropertyValues("ai.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DeploymentDiagnosisService.class);
            assertThat(context).hasSingleBean(DeploymentDiagnosisController.class);
            assertThat(context).hasSingleBean(OrphanDiagnosisCleaner.class);
        });
    }

    @Test
    @DisplayName("토글이 꺼져 있으면 진단 빈이 하나도 등록되지 않는다")
    void registersNothingWhenDisabled() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DeploymentDiagnosisService.class);
            assertThat(context).doesNotHaveBean(DeploymentDiagnosisController.class);
            assertThat(context).doesNotHaveBean(OrphanDiagnosisCleaner.class);
        });
    }

    @Test
    @DisplayName("ai.enabled=false를 명시해도 등록되지 않는다")
    void registersNothingWhenExplicitlyFalse() {
        runner.withPropertyValues("ai.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DeploymentDiagnosisService.class);
        });
    }

    /**
     * 리포지토리와 Claude 클라이언트는 목으로 대체한다. 여기서 확인하려는 것은
     * "조립이 되는가"이지 실제 동작이 아니다.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({
            DeploymentDiagnosisService.class,
            DeploymentDiagnosisController.class,
            OrphanDiagnosisCleaner.class,
            FailureLogSlicer.class,
            DiagnosisPromptBuilder.class,
            SecretRedactor.class
    })
    static class DiagnosisTestConfig {

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
        ClaudeClient claudeClient() {
            return mock(ClaudeClient.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
