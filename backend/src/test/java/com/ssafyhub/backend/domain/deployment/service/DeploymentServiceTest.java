package com.ssafyhub.backend.domain.deployment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssafyhub.backend.domain.artifact.entity.Artifact;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactStatus;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactType;
import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateResponse;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentEnvRequest;
import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentEnv;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.domain.deployment.outbox.client.DevOpsDeploymentClient;
import com.ssafyhub.backend.domain.deployment.outbox.entity.DeploymentOutbox;
import com.ssafyhub.backend.domain.deployment.outbox.enums.OutboxStatus;
import com.ssafyhub.backend.domain.deployment.outbox.repository.DeploymentOutboxRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.deployment.support.S3ObjectVerifier;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import com.ssafyhub.backend.global.security.SecretEncryptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ArtifactRepository artifactRepository;
    @Mock
    private DeploymentRepository deploymentRepository;
    @Mock
    private DeploymentEnvRepository deploymentEnvRepository;
    @Mock
    private DeploymentLogRepository deploymentLogRepository;
    @Mock
    private DeploymentOutboxRepository deploymentOutboxRepository;
    @Mock
    private DevOpsDeploymentClient devOpsDeploymentClient;
    @Mock
    private S3ObjectVerifier s3ObjectVerifier;

    private SecretEncryptor secretEncryptor;
    private DeploymentService deploymentService;
    private Member member;
    private Artifact backendArtifact;
    private Artifact frontendArtifact;

    @BeforeEach
    void setUp() {
        secretEncryptor = new SecretEncryptor("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        deploymentService = new DeploymentService(
                memberRepository,
                artifactRepository,
                deploymentRepository,
                deploymentEnvRepository,
                deploymentLogRepository,
                deploymentOutboxRepository,
                devOpsDeploymentClient,
                s3ObjectVerifier,
                secretEncryptor
        );
        ReflectionTestUtils.setField(deploymentService, "publicDomain", "ssafyhub.site");

        member = new Member("1551343", "hash", "홍길동", "E206");
        ReflectionTestUtils.setField(member, "id", 11L);
        backendArtifact = artifact(1L, ArtifactType.BACKEND_JAR, "backend/app.jar");
        frontendArtifact = artifact(2L, ArtifactType.FRONTEND_ZIP, "frontend/dist.zip");
    }

    @Test
    void 배포_요청과_환경변수_로그_Outbox를_저장한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(backendArtifact));
        given(artifactRepository.findById(2L)).willReturn(Optional.of(frontendArtifact));
        given(deploymentRepository.saveAndFlush(any(Deployment.class))).willAnswer(invocation -> {
            Deployment deployment = invocation.getArgument(0);
            ReflectionTestUtils.setField(deployment, "id", 100L);
            return deployment;
        });

        DeploymentCreateResponse response = deploymentService.createDeployment(11L, validRequest());

        assertThat(response.deploymentId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(DeploymentStatus.PENDING);
        assertThat(response.displayStatus()).isEqualTo("배포 대기 중");
        assertThat(response.expectedUrl()).isEqualTo("https://1551343.ssafyhub.site");

        ArgumentCaptor<Deployment> deploymentCaptor = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentRepository).saveAndFlush(deploymentCaptor.capture());
        Deployment deployment = deploymentCaptor.getValue();
        assertThat(deployment.getSlug()).isEqualTo("1551343");
        assertThat(deployment.getEncryptedDatabasePassword())
                .startsWith("v1:")
                .doesNotContain("db-password");

        ArgumentCaptor<List<DeploymentEnv>> envCaptor = ArgumentCaptor.forClass(List.class);
        verify(deploymentEnvRepository).saveAll(envCaptor.capture());
        assertThat(envCaptor.getValue()).hasSize(2);
        assertThat(envCaptor.getValue())
                .extracting(DeploymentEnv::getEncryptedValue)
                .allSatisfy(value -> assertThat(value).startsWith("v1:").doesNotContain("secret-value"));

        ArgumentCaptor<DeploymentLog> logCaptor = ArgumentCaptor.forClass(DeploymentLog.class);
        verify(deploymentLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(DeploymentStatus.PENDING);

        ArgumentCaptor<DeploymentOutbox> outboxCaptor = ArgumentCaptor.forClass(DeploymentOutbox.class);
        verify(deploymentOutboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getDeploymentId()).isEqualTo(100L);
        assertThat(outboxCaptor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxCaptor.getValue().getPayload()).isEqualTo("{\"deploymentId\":100}");

        assertThat(backendArtifact.getStatus()).isEqualTo(ArtifactStatus.USED);
        assertThat(frontendArtifact.getStatus()).isEqualTo(ArtifactStatus.USED);
        verify(s3ObjectVerifier).verifyExists("ssafyhub-artifacts", "backend/app.jar");
        verify(s3ObjectVerifier).verifyExists("ssafyhub-artifacts", "frontend/dist.zip");
    }

    @Test
    void 이미_배포가_있으면_요청을_거부한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(true);

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, validRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DEPLOYMENT_ALREADY_EXISTS);

        verify(artifactRepository, never()).findById(any());
        verify(deploymentOutboxRepository, never()).save(any());
    }

    @Test
    void 중복된_환경변수_이름은_거부한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(backendArtifact));
        given(artifactRepository.findById(2L)).willReturn(Optional.of(frontendArtifact));
        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", 1L, 2L, "SPRING_BOOT", "21", 8080, "app_db", "db-password",
                null,
                null, null, null, null, null,
                false,
                null,
                null,
                List.of(
                        new DeploymentEnvRequest("JWT_SECRET", "one"),
                        new DeploymentEnvRequest("JWT_SECRET", "two")
                ),
                null,
                null
        );

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ENV_KEY);

        verify(s3ObjectVerifier, never()).verifyExists(any(), any());
    }

    @Test
    void 다른_회원의_Artifact는_사용할_수_없다() {
        Member otherMember = new Member("1551344", "hash", "다른회원", "E207");
        ReflectionTestUtils.setField(otherMember, "id", 12L);
        Artifact otherArtifact = new Artifact(
                otherMember,
                ArtifactType.BACKEND_JAR,
                "app.jar",
                "ssafyhub-artifacts",
                "other/app.jar",
                LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(otherArtifact, "id", 3L);

        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(otherArtifact));

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, validRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ARTIFACT_ACCESS_DENIED);
    }

    private Artifact artifact(Long id, ArtifactType type, String objectKey) {
        Artifact artifact = new Artifact(
                member,
                type,
                objectKey.substring(objectKey.lastIndexOf('/') + 1),
                "ssafyhub-artifacts",
                objectKey,
                LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(artifact, "id", id);
        return artifact;
    }

    @Test
    void 프론트엔드_아티팩트_없이도_배포를_생성한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(backendArtifact));
        given(deploymentRepository.saveAndFlush(any(Deployment.class))).willAnswer(invocation -> {
            Deployment deployment = invocation.getArgument(0);
            ReflectionTestUtils.setField(deployment, "id", 100L);
            return deployment;
        });

        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", 1L, null, "SPRING_BOOT", "21", 8080, "app_db", "db-password",
                null, null, null, null, null, null, false, null, null,
                List.of(new DeploymentEnvRequest("JWT_SECRET", "secret-value")),
                null, null
        );

        DeploymentCreateResponse response = deploymentService.createDeployment(11L, request);

        assertThat(response.deploymentId()).isEqualTo(100L);
        verify(artifactRepository, never()).findById(2L);
        verify(s3ObjectVerifier, never()).verifyExists("ssafyhub-artifacts", "frontend/dist.zip");

        ArgumentCaptor<Deployment> deploymentCaptor = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentRepository).saveAndFlush(deploymentCaptor.capture());
        assertThat(deploymentCaptor.getValue().getFrontendArtifact()).isNull();
    }

    @Test
    void 백엔드_없이_프론트만으로도_배포를_생성한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(2L)).willReturn(Optional.of(frontendArtifact));
        given(deploymentRepository.saveAndFlush(any(Deployment.class))).willAnswer(invocation -> {
            Deployment deployment = invocation.getArgument(0);
            ReflectionTestUtils.setField(deployment, "id", 100L);
            return deployment;
        });

        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", null, 2L, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of(new DeploymentEnvRequest("VITE_API_BASE", "https://api.example.com")),
                null, null
        );

        DeploymentCreateResponse response = deploymentService.createDeployment(11L, request);

        assertThat(response.deploymentId()).isEqualTo(100L);
        verify(artifactRepository, never()).findById(1L);

        ArgumentCaptor<Deployment> deploymentCaptor = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentRepository).saveAndFlush(deploymentCaptor.capture());
        assertThat(deploymentCaptor.getValue().getBackendArtifact()).isNull();
    }

    @Test
    void 백엔드도_프론트도_없으면_거부한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);

        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of(new DeploymentEnvRequest("A", "b")),
                null, null
        );

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NEED_AT_LEAST_ONE_ARTIFACT);

        verify(deploymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void 수동_마이그레이션인데_스키마_SQL이_없으면_거부한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(backendArtifact));
        given(artifactRepository.findById(2L)).willReturn(Optional.of(frontendArtifact));

        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", 1L, 2L, "SPRING_BOOT", "21", 8080, "app_db", "db-password",
                "MYSQL", null, null, null, null, null, true, null, null,
                List.of(new DeploymentEnvRequest("SPRING_PROFILES_ACTIVE", "prod")),
                null, null
        );

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SCHEMA_REQUIRED_WHEN_MANUAL_MIGRATION);

        verify(deploymentRepository, never()).saveAndFlush(any());
        verify(deploymentOutboxRepository, never()).save(any());
    }

    @Test
    void 자동_마이그레이션인데_스키마_SQL을_보내면_거부한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(backendArtifact));
        given(artifactRepository.findById(2L)).willReturn(Optional.of(frontendArtifact));

        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", 1L, 2L, "SPRING_BOOT", "21", 8080, "app_db", "db-password",
                "MYSQL", null, null, null, null, null, false, 9L, null,
                List.of(new DeploymentEnvRequest("SPRING_PROFILES_ACTIVE", "prod")),
                null, null
        );

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SCHEMA_NOT_ALLOWED_WHEN_AUTO_MIGRATION);

        verify(deploymentRepository, never()).saveAndFlush(any());
        verify(deploymentOutboxRepository, never()).save(any());
    }

    @Test
    void DB를_쓰는데_마이그레이션_여부가_없으면_거부한다() {
        given(memberRepository.findById(11L)).willReturn(Optional.of(member));
        given(deploymentRepository.existsByMemberId(11L)).willReturn(false);
        given(artifactRepository.findById(1L)).willReturn(Optional.of(backendArtifact));
        given(artifactRepository.findById(2L)).willReturn(Optional.of(frontendArtifact));

        DeploymentCreateRequest request = new DeploymentCreateRequest(
                "demo-service", 1L, 2L, "SPRING_BOOT", "21", 8080, "app_db", "db-password",
                "MYSQL", null, null, null, null, null, null, null, null,
                List.of(new DeploymentEnvRequest("SPRING_PROFILES_ACTIVE", "prod")),
                null, null
        );

        assertThatThrownBy(() -> deploymentService.createDeployment(11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MIGRATION_TYPE_REQUIRED);

        verify(deploymentRepository, never()).saveAndFlush(any());
        verify(deploymentOutboxRepository, never()).save(any());
    }

    private DeploymentCreateRequest validRequest() {
        return new DeploymentCreateRequest(
                "demo-service",
                1L,
                2L,
                "SPRING_BOOT",
                "21",
                8080,
                "app_db",
                "db-password",
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                List.of(
                        new DeploymentEnvRequest("SPRING_PROFILES_ACTIVE", "prod"),
                        new DeploymentEnvRequest("JWT_SECRET", "secret-value")
                ),
                null,
                null
        );
    }
}
