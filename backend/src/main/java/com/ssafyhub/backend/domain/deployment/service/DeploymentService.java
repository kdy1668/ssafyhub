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
import com.ssafyhub.backend.domain.deployment.outbox.client.DevOpsDeploymentClient;
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
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private static final String PENDING_MESSAGE = "배포 요청이 접수되었습니다.";
    private static final Set<String> SUPPORTED_NEEDED_SERVICES = Set.of("REDIS", "MONGODB");
    private static final Set<String> SUPPORTED_DATABASE_ENGINES = Set.of("MYSQL");
    // Java 계열(.jar 아티팩트)과 Python/Node.js 계열(.zip 소스 아티팩트)을 구분해야 파일 확장자를 검증할 수 있다.
    // JPA/MyBatis는 더 이상 별도 스택이 아니다 - 둘 다 .jar 하나 받아 "java -jar"로 실행하는 것뿐이라
    // Dockerfile이 완전히 같고(JAR_STACKS 판정에도 차이가 없었음), 차이는 오직 "테이블을 자동으로
    // 만들어주는가"였는데 그건 이제 프레임워크와 무관하게 사용자에게 직접 물어본다(manualMigration).
    private static final Set<String> JAR_STACKS = Set.of("SPRING_BOOT");
    // FLASK는 Django/FastAPI와 실제로 다르다 - WSGI라 gunicorn으로 띄워야 해서(uvicorn 아님)
    // 플랫폼이 생성하는 Dockerfile의 CMD 자체가 다르다. Node는 EXPRESS 하나로 표기하지만
    // Dockerfile이 "npm start"에 위임하는 방식이라 실제로는 Fastify/Koa/NestJS 등도 그대로 동작한다.
    private static final Set<String> SUPPORTED_STACKS = Set.of("SPRING_BOOT", "DJANGO", "FASTAPI", "FLASK", "EXPRESS");

    private final MemberRepository memberRepository;
    private final ArtifactRepository artifactRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentEnvRepository deploymentEnvRepository;
    private final DeploymentLogRepository deploymentLogRepository;
    private final DeploymentOutboxRepository deploymentOutboxRepository;
    private final DevOpsDeploymentClient devOpsDeploymentClient;
    private final S3ObjectVerifier s3ObjectVerifier;
    private final SecretEncryptor secretEncryptor;

    @Value("${deployment.public-domain}")
    private String publicDomain;

    @Transactional
    public DeploymentCreateResponse createDeployment(Long memberId, DeploymentCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (deploymentRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.DEPLOYMENT_ALREADY_EXISTS);
        }

        // 백엔드/프론트 둘 다 선택 사항이지만 최소 하나는 있어야 한다 - 아무것도 없는 배포는 무의미.
        if (request.backendArtifactId() == null && request.frontendArtifactId() == null) {
            throw new BusinessException(ErrorCode.NEED_AT_LEAST_ONE_ARTIFACT);
        }

        Artifact backendArtifact = null;
        String stack = null;
        String runtimeVersion = null;
        Integer internalPort = null;
        if (request.backendArtifactId() != null) {
            backendArtifact = getArtifact(request.backendArtifactId());
            validateArtifact(backendArtifact, memberId, ArtifactType.BACKEND_JAR);
            stack = validateStack(request.stack(), backendArtifact);
            runtimeVersion = blankToNull(request.runtimeVersion());
            if (runtimeVersion == null) {
                throw new BusinessException(ErrorCode.RUNTIME_VERSION_REQUIRED);
            }
            if (request.internalPort() == null || request.internalPort() < 1 || request.internalPort() > 65535) {
                throw new BusinessException(ErrorCode.INTERNAL_PORT_REQUIRED);
            }
            internalPort = request.internalPort();
        }

        // 프론트엔드는 선택 사항 - 백엔드가 프론트를 직접 서빙하는 모놀리식 구조라면(또는 백엔드
        // 자체가 없는 프론트만 배포라면) 안 보낸다. 항상 이미 빌드된 정적 zip을 그대로 올린다
        // (플랫폼이 대신 빌드해주지 않음 - 프레임워크와 무관하게 정적 파일이라 구분할 이유가 없다).
        Artifact frontendArtifact = null;
        if (request.frontendArtifactId() != null) {
            frontendArtifact = getArtifact(request.frontendArtifactId());
            validateArtifact(frontendArtifact, memberId, ArtifactType.FRONTEND_ZIP);
        }
        validateUniqueEnvKeys(request.env());

        String databaseName = blankToNull(request.databaseName());
        String databasePassword = blankToNull(request.databasePassword());
        if (databaseName != null && backendArtifact == null) {
            throw new BusinessException(ErrorCode.DATABASE_REQUIRES_BACKEND);
        }
        if (databaseName != null && databasePassword == null) {
            throw new BusinessException(ErrorCode.DATABASE_PASSWORD_REQUIRED);
        }
        String encryptedDatabasePassword = databasePassword != null ? secretEncryptor.encrypt(databasePassword) : null;
        String databaseEngine = validateDatabaseEngine(databaseName, request.databaseEngine());
        String neededServices = validateAndJoinNeededServices(request.neededServices());

        // 테이블 자동 생성 여부는 더 이상 스택에서 추론하지 않고 사용자에게 직접 물어본다 - 같은
        // Express라도 Sequelize sync()면 자동, 직접 SQL이면 수동일 수 있어서 프레임워크만으로는
        // 알 수 없다. DB를 쓰는 백엔드 배포에서만 의미가 있다.
        Boolean manualMigration = null;
        if (backendArtifact != null && databaseName != null) {
            manualMigration = request.manualMigration();
            if (manualMigration == null) {
                throw new BusinessException(ErrorCode.MIGRATION_TYPE_REQUIRED);
            }
        }

        Artifact schemaArtifact = null;
        if (request.schemaArtifactId() != null) {
            if (backendArtifact == null || databaseName == null) {
                throw new BusinessException(ErrorCode.SCHEMA_REQUIRES_DATABASE);
            }
            if (!Boolean.TRUE.equals(manualMigration)) {
                throw new BusinessException(ErrorCode.SCHEMA_NOT_ALLOWED_WHEN_AUTO_MIGRATION);
            }
            schemaArtifact = getArtifact(request.schemaArtifactId());
            validateArtifact(schemaArtifact, memberId, ArtifactType.SCHEMA_SQL);
        } else if (Boolean.TRUE.equals(manualMigration)) {
            throw new BusinessException(ErrorCode.SCHEMA_REQUIRED_WHEN_MANUAL_MIGRATION);
        }

        if (backendArtifact != null) {
            s3ObjectVerifier.verifyExists(backendArtifact.getBucket(), backendArtifact.getObjectKey());
        }
        if (frontendArtifact != null) {
            s3ObjectVerifier.verifyExists(frontendArtifact.getBucket(), frontendArtifact.getObjectKey());
        }
        if (schemaArtifact != null) {
            s3ObjectVerifier.verifyExists(schemaArtifact.getBucket(), schemaArtifact.getObjectKey());
        }

        LocalDateTime requestedAt = LocalDateTime.now();

        Deployment deployment = new Deployment(
                member,
                backendArtifact,
                frontendArtifact,
                request.projectName().trim(),
                member.getStudentNumber(),
                runtimeVersion,
                stack,
                internalPort,
                databaseName,
                encryptedDatabasePassword,
                databaseEngine,
                blankToNull(request.dbHostEnvKey()),
                blankToNull(request.dbPortEnvKey()),
                blankToNull(request.dbNameEnvKey()),
                blankToNull(request.dbUsernameEnvKey()),
                blankToNull(request.dbPasswordEnvKey()),
                schemaArtifact,
                neededServices,
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

        List<Artifact> usedArtifacts = new java.util.ArrayList<>();
        if (backendArtifact != null) {
            backendArtifact.markUsed();
            usedArtifacts.add(backendArtifact);
        }
        if (frontendArtifact != null) {
            frontendArtifact.markUsed();
            usedArtifacts.add(frontendArtifact);
        }
        if (schemaArtifact != null) {
            schemaArtifact.markUsed();
            usedArtifacts.add(schemaArtifact);
        }
        artifactRepository.saveAll(usedArtifacts);

        deploymentOutboxRepository.save(DeploymentOutbox.deploymentRequested(deployment.getId(), requestedAt));

        return new DeploymentCreateResponse(
                deployment.getId(),
                DeploymentStatus.PENDING,
                DeploymentStatus.PENDING.displayStatus(),
                "https://%s.%s".formatted(member.getStudentNumber(), publicDomain),
                requestedAt
        );
    }

    /**
     * 배포된 서비스의 리소스 사용량(현재 스냅샷) - deploy-engine 메트릭을 프론트 형태로 매핑.
     * deploy-engine이 값이 없으면(미배포/스크레이프 전) null → 0으로 처리해 500 대신 정상 응답한다.
     * deploy-engine 자체 호출 실패 시에도 0으로 응답(리소스 확인이 화면을 깨지 않도록).
     */
    public com.ssafyhub.backend.domain.deployment.dto.ResourceUsageResponse getResourceUsage(
            Long memberId, Long deploymentId) {
        Deployment deployment = deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        com.ssafyhub.backend.domain.deployment.dto.DevOpsMetricsResponse metrics;
        try {
            metrics = devOpsDeploymentClient.getMetrics(deployment.getId());
        } catch (RestClientException exception) {
            metrics = null; // deploy-engine 순단 등 - 0으로 채워 응답
        }

        int cpuPercent = 0;
        long memoryUsedMb = 0;
        long networkReceiveBytes = 0;
        long networkTransmitBytes = 0;
        long diskUsageBytes = 0;
        long requestsTotal = 0;
        if (metrics != null) {
            if (metrics.cpuCores() != null) {
                cpuPercent = (int) Math.round(metrics.cpuCores() * 100); // 코어 사용량 → % (1코어=100%)
            }
            if (metrics.memoryBytes() != null) {
                memoryUsedMb = metrics.memoryBytes() / (1024 * 1024);
            }
            if (metrics.networkReceiveBytes() != null) {
                networkReceiveBytes = metrics.networkReceiveBytes();
            }
            if (metrics.networkTransmitBytes() != null) {
                networkTransmitBytes = metrics.networkTransmitBytes();
            }
            if (metrics.diskUsageBytes() != null) {
                diskUsageBytes = metrics.diskUsageBytes();
            }
            if (metrics.requestsTotal() != null) {
                requestsTotal = metrics.requestsTotal();
            }
        }
        // deploy-engine이 컨테이너 메모리 상한을 512Mi 고정으로 배포하므로 총량은 512로 본다.
        long memoryTotalMb = 512;

        return new com.ssafyhub.backend.domain.deployment.dto.ResourceUsageResponse(
                deployment.getId(), cpuPercent, memoryUsedMb, memoryTotalMb,
                networkReceiveBytes, networkTransmitBytes, diskUsageBytes, requestsTotal,
                LocalDateTime.now());
    }

    /**
     * 배포 리소스 사용량 시계열(그래프용) - deploy-engine /metrics/range를 CPU%(코어*100)/메모리MB로 변환.
     * deploy-engine 순단/무데이터 시 빈 시계열로 응답(화면이 깨지지 않도록).
     */
    public com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse getResourceHistory(
            Long memberId, Long deploymentId, int minutes) {
        Deployment deployment = deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        com.ssafyhub.backend.domain.deployment.dto.DevOpsMetricsRangeResponse range;
        try {
            range = devOpsDeploymentClient.getMetricsRange(deployment.getId(), minutes, 60);
        } catch (RestClientException exception) {
            range = null;
        }

        java.util.List<com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint> cpu =
                new java.util.ArrayList<>();
        java.util.List<com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint> memory =
                new java.util.ArrayList<>();
        java.util.List<com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint> networkReceive =
                new java.util.ArrayList<>();
        java.util.List<com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint> networkTransmit =
                new java.util.ArrayList<>();
        java.util.List<com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint> disk =
                new java.util.ArrayList<>();
        java.util.List<com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint> requests =
                new java.util.ArrayList<>();
        if (range != null) {
            if (range.cpuCores() != null) {
                range.cpuCores().forEach(p -> cpu.add(
                        new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint(
                                p.epochSeconds(), p.value() != null ? p.value() * 100 : 0)));
            }
            if (range.memoryBytes() != null) {
                range.memoryBytes().forEach(p -> memory.add(
                        new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint(
                                p.epochSeconds(), p.value() != null ? p.value() / (1024.0 * 1024.0) : 0)));
            }
            if (range.networkReceiveBytes() != null) {
                range.networkReceiveBytes().forEach(p -> networkReceive.add(
                        new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint(
                                p.epochSeconds(), p.value() != null ? p.value() / (1024.0 * 1024.0) : 0)));
            }
            if (range.networkTransmitBytes() != null) {
                range.networkTransmitBytes().forEach(p -> networkTransmit.add(
                        new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint(
                                p.epochSeconds(), p.value() != null ? p.value() / (1024.0 * 1024.0) : 0)));
            }
            if (range.diskUsageBytes() != null) {
                range.diskUsageBytes().forEach(p -> disk.add(
                        new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint(
                                p.epochSeconds(), p.value() != null ? p.value() / (1024.0 * 1024.0) : 0)));
            }
            if (range.requestsTotal() != null) {
                range.requestsTotal().forEach(p -> requests.add(
                        new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse.SeriesPoint(
                                p.epochSeconds(), p.value() != null ? p.value() : 0)));
            }
        }
        return new com.ssafyhub.backend.domain.deployment.dto.ResourceHistoryResponse(
                cpu, memory, networkReceive, networkTransmit, disk, requests);
    }

    /** 배포 진행 로그(깃헙 액션식 단계별) - 본인 배포만 조회 가능. */
    public java.util.List<com.ssafyhub.backend.domain.deployment.dto.DeploymentLogResponse> getDeploymentLogs(
            Long memberId, Long deploymentId) {
        Deployment deployment = deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        return deploymentLogRepository.findByDeployment_IdOrderByIdAsc(deployment.getId())
                .stream()
                .map(com.ssafyhub.backend.domain.deployment.dto.DeploymentLogResponse::from)
                .toList();
    }

    public DeploymentStatusResponse getMyDeployment(Long memberId) {
        Deployment deployment = deploymentRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        DeploymentLog latestLog = deploymentLogRepository.findFirstByDeployment_IdOrderByIdDesc(deployment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // 실제로 떠서(RUNNING) 접속 가능할 때만 접속 주소를 서브도메인으로 노출한다.
        // endpoint_url(IP+경로)은 백엔드가 있을 때만 채워지는 프록시 내부 타깃이라, 프론트만
        // 배포한 경우(hasBackend=false)는 절대 채워지지 않는다 - endpoint_url 존재 여부로
        // 판단하면 프론트만 있는 배포는 정상적으로 떠 있어도 URL이 영원히 안 보이게 된다.
        // 실제 접속 가능 여부는 최신 로그 상태(RUNNING)로 판단해야 한다.
        String accessUrl = latestLog.getStatus() == DeploymentStatus.RUNNING
                ? "https://%s.%s".formatted(deployment.getSlug(), publicDomain)
                : null;

        return DeploymentStatusResponse.of(deployment, latestLog, accessUrl);
    }

    /**
     * 배포를 삭제한다. 실행 중인 사용자 서버(k8s Deployment/Service/Ingress/Middleware)를 먼저
     * 내리고, 성공한 뒤에 DB 행을 지운다.
     *
     * 순서가 중요하다. deploy-engine은 deploymentId로 namespace(팀)와 appName(slug)을 역추적하므로,
     * 행을 먼저 지우면 정리 대상을 찾지 못해 k8s 리소스가 그대로 떠 있는 채로 고아가 된다.
     * 반대로 k8s 정리 후 행 삭제가 실패하면 사용자가 다시 삭제를 시도하면 되고,
     * deploy-engine의 정리는 멱등이므로 안전하다.
     */
    @Transactional
    public void deleteDeployment(Long memberId, Long deploymentId) {
        Deployment deployment = deploymentRepository.findByIdAndMember_Id(deploymentId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        try {
            devOpsDeploymentClient.deleteDeployment(deployment.getId());
        } catch (RestClientException exception) {
            // k8s 리소스가 살아 있을 수 있으므로 행을 지우지 않고 중단한다.
            throw new BusinessException(ErrorCode.DEPLOYMENT_DELETE_FAILED);
        }

        // deployment_envs / deployment_logs는 FK가 걸려 있어 먼저 지워야 하고,
        // deployment_outbox는 FK가 없지만 남으면 스케줄러가 계속 발행을 시도한다.
        deploymentOutboxRepository.deleteByDeploymentId(deployment.getId());
        deploymentEnvRepository.deleteByDeployment_Id(deployment.getId());
        deploymentLogRepository.deleteByDeployment_Id(deployment.getId());
        deploymentRepository.delete(deployment);
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

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * stack이 지원 목록에 있는지, 그리고 백엔드 아티팩트 확장자가 그 스택과 맞는지 검증한다.
     * Java 계열(.jar 자동 Dockerfile)과 Python/Node.js 계열(.zip 소스 + 자동 Dockerfile)은
     * 업로드 형식 자체가 다르므로, 잘못 짝지어지면 배포 단계에서야 실패하지 않도록 여기서 막는다.
     */
    private String validateStack(String rawStack, Artifact backendArtifact) {
        String stack = rawStack == null ? "" : rawStack.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STACKS.contains(stack)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_STACK);
        }

        String fileName = backendArtifact.getOriginalFileName().toLowerCase(Locale.ROOT);
        boolean isJavaStack = JAR_STACKS.contains(stack);
        boolean isJarFile = fileName.endsWith(".jar");
        if (isJavaStack != isJarFile) {
            throw new BusinessException(ErrorCode.STACK_ARTIFACT_MISMATCH);
        }
        return stack;
    }

    /** databaseName이 없으면 엔진도 의미 없으니 null. 있는데 엔진이 비어 있으면 MYSQL 기본값. */
    private String validateDatabaseEngine(String databaseName, String rawEngine) {
        if (databaseName == null) {
            return null;
        }
        String engine = blankToNull(rawEngine);
        if (engine == null) {
            return "MYSQL";
        }
        String upper = engine.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_DATABASE_ENGINES.contains(upper)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_DATABASE_ENGINE);
        }
        return upper;
    }

    private String validateAndJoinNeededServices(List<String> neededServices) {
        if (neededServices == null || neededServices.isEmpty()) {
            return null;
        }
        Set<String> normalized = new HashSet<>();
        for (String service : neededServices) {
            String upper = service == null ? "" : service.trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_NEEDED_SERVICES.contains(upper)) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_NEEDED_SERVICE);
            }
            normalized.add(upper);
        }
        return String.join(",", normalized);
    }

    private void validateUniqueEnvKeys(List<DeploymentEnvRequest> envs) {
        Set<String> keys = new HashSet<>();
        boolean hasDuplicate = envs.stream().map(DeploymentEnvRequest::key).anyMatch(key -> !keys.add(key));

        if (hasDuplicate) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENV_KEY);
        }
    }
}
