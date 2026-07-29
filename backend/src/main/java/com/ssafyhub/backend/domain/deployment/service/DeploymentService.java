package com.ssafyhub.backend.domain.deployment.service;

import com.ssafyhub.backend.domain.artifact.entity.Artifact;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactStatus;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactType;
import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentCreateResponse;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentEnvRequest;
import com.ssafyhub.backend.domain.deployment.dto.DeploymentStatusResponse;
import com.ssafyhub.backend.domain.deployment.entity.Deployment;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentEnv;
import com.ssafyhub.backend.domain.deployment.entity.DeploymentLog;
import com.ssafyhub.backend.domain.deployment.enums.DeploymentStatus;
import com.ssafyhub.backend.domain.deployment.outbox.entity.DeploymentOutbox;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private static final String PENDING_MESSAGE = "배포 요청이 접수되었습니다.";

    private final MemberRepository memberRepository;
    private final ArtifactRepository artifactRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentEnvRepository deploymentEnvRepository;
    private final DeploymentLogRepository deploymentLogRepository;
    private final DeploymentOutboxRepository deploymentOutboxRepository;
    private final S3ObjectVerifier s3ObjectVerifier;
    private final SecretEncryptor secretEncryptor;

    @Value("${deployment.supported-runtime-versions}")
    private List<String> supportedRuntimeVersions;

    @Value("${deployment.public-domain}")
    private String publicDomain;

    @Transactional
    public DeploymentCreateResponse createDeployment(Long memberId, DeploymentCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (deploymentRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_ALREADY_EXISTS);
        }

        validateRuntimeVersion(request.runtimeVersion());
        validateUniqueEnvKeys(request.env());

        Artifact backendArtifact = getArtifact(request.backendArtifactId());
        Artifact frontendArtifact = getArtifact(request.frontendArtifactId());

        validateArtifact(backendArtifact, memberId, ArtifactType.BACKEND_JAR);
        validateArtifact(frontendArtifact, memberId, ArtifactType.FRONTEND_ZIP);

        s3ObjectVerifier.verifyExists(backendArtifact.getBucket(), backendArtifact.getObjectKey());
        s3ObjectVerifier.verifyExists(frontendArtifact.getBucket(), frontendArtifact.getObjectKey());

        LocalDateTime requestedAt = LocalDateTime.now();
        String encryptedDatabasePassword = secretEncryptor.encrypt(request.databasePassword());

        Deployment deployment = new Deployment(
                member,
                backendArtifact,
                frontendArtifact,
                request.projectName().trim(),
                member.getStudentNumber(),
                request.runtimeVersion().trim(),
                request.internalPort(),
                request.databaseName(),
                encryptedDatabasePassword,
                requestedAt,
                request.visibility(),
                request.description()
        );

        try {
            deploymentRepository.saveAndFlush(deployment);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_ALREADY_EXISTS);
        }

        List<DeploymentEnv> deploymentEnvs = request.env().stream()
                .map(env -> new DeploymentEnv(
                        deployment,
                        env.key(),
                        secretEncryptor.encrypt(env.value())
                ))
                .toList();
        deploymentEnvRepository.saveAll(deploymentEnvs);

        deploymentLogRepository.save(new DeploymentLog(
                deployment,
                DeploymentStatus.PENDING,
                PENDING_MESSAGE
        ));

        backendArtifact.markUsed();
        frontendArtifact.markUsed();
        artifactRepository.saveAll(List.of(backendArtifact, frontendArtifact));

        deploymentOutboxRepository.save(DeploymentOutbox.deploymentRequested(deployment.getId(), requestedAt));

        return new DeploymentCreateResponse(
                deployment.getId(),
                DeploymentStatus.PENDING,
                DeploymentStatus.PENDING.displayStatus(),
                "https://%s.%s".formatted(member.getStudentNumber(), publicDomain),
                requestedAt
        );
    }

    public DeploymentStatusResponse getMyDeployment(Long memberId) {
        Deployment deployment = deploymentRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        DeploymentLog latestLog = deploymentLogRepository.findFirstByDeployment_IdOrderByIdDesc(deployment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        return DeploymentStatusResponse.of(deployment, latestLog);
    }

    private Artifact getArtifact(Long artifactId) {
        return artifactRepository.findById(artifactId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND));
    }

    private void validateArtifact(Artifact artifact, Long memberId, ArtifactType expectedType) {
        if (!artifact.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ARTIFACT_ACCESS_DENIED);
        }

        if (artifact.getArtifactType() != expectedType) {
            throw new BusinessException(ErrorCode.ARTIFACT_TYPE_MISMATCH);
        }

        if (artifact.getStatus() != ArtifactStatus.URL_ISSUED) {
            throw new BusinessException(ErrorCode.ARTIFACT_ALREADY_USED);
        }
    }

    private void validateRuntimeVersion(String runtimeVersion) {
        if (!supportedRuntimeVersions.contains(runtimeVersion.trim())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_RUNTIME_VERSION);
        }
    }

    private void validateUniqueEnvKeys(List<DeploymentEnvRequest> envs) {
        Set<String> keys = new HashSet<>();
        boolean hasDuplicate = envs.stream().map(DeploymentEnvRequest::key).anyMatch(key -> !keys.add(key));

        if (hasDuplicate) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENV_KEY);
        }
    }
}
