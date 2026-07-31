package com.ssafyhub.backend.domain.artifact.service;

import com.ssafyhub.backend.domain.artifact.dto.ArtifactUploadResponse;
import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlRequest;
import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlResponse;
import com.ssafyhub.backend.domain.artifact.entity.Artifact;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactType;
import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.artifact.support.S3PresignedUrlProvider;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final MemberRepository memberRepository;
    private final ArtifactRepository artifactRepository;
    private final S3PresignedUrlProvider presignedUrlProvider;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presigned-url-expiration-minutes}")
    private long expirationMinutes;

    @Transactional
    public PresignedUrlResponse issuePresignedUrls(Long memberId, PresignedUrlRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 백엔드: .jar(플랫폼이 표준 Dockerfile 자동 생성) 또는 .zip(Dockerfile 포함 BYO 빌드) 둘 다 허용.
        String backendFileName = validateFileName(
                request.backendFileName(), List.of(".jar", ".zip"), ErrorCode.INVALID_BACKEND_FILE);

        String requestId = UUID.randomUUID().toString();
        String backendObjectKey = createObjectKey(memberId, requestId, "backend", backendFileName);
        Duration signatureDuration = Duration.ofMinutes(expirationMinutes);
        LocalDateTime expiresAt = LocalDateTime.now().plus(signatureDuration);

        Artifact backendArtifact = new Artifact(
                member,
                ArtifactType.BACKEND_JAR,
                backendFileName,
                bucket,
                backendObjectKey,
                expiresAt
        );

        // 프론트엔드는 선택 사항 - 백엔드가 프론트를 직접 서빙하는 모놀리식 구조라면 안 보낸다.
        boolean needsFrontend = request.frontendFileName() != null && !request.frontendFileName().isBlank();
        Artifact frontendArtifact = null;
        if (needsFrontend) {
            String frontendFileName = validateFileName(
                    request.frontendFileName(), List.of(".zip"), ErrorCode.INVALID_FRONTEND_FILE);
            String frontendObjectKey = createObjectKey(memberId, requestId, "frontend", frontendFileName);
            frontendArtifact = new Artifact(
                    member,
                    ArtifactType.FRONTEND_ZIP,
                    frontendFileName,
                    bucket,
                    frontendObjectKey,
                    expiresAt
            );
        }

        // DB 초기화 SQL은 선택 사항 - MyBatis 등 자동 테이블 생성이 안 되는 프레임워크를 쓸 때만 보낸다.
        boolean needsSchema = request.schemaFileName() != null && !request.schemaFileName().isBlank();
        Artifact schemaArtifact = null;
        if (needsSchema) {
            String schemaFileName = validateFileName(
                    request.schemaFileName(), List.of(".sql"), ErrorCode.INVALID_SCHEMA_FILE);
            String schemaObjectKey = createObjectKey(memberId, requestId, "schema", schemaFileName);
            schemaArtifact = new Artifact(
                    member,
                    ArtifactType.SCHEMA_SQL,
                    schemaFileName,
                    bucket,
                    schemaObjectKey,
                    expiresAt
            );
        }

        List<Artifact> artifacts = new java.util.ArrayList<>(List.of(backendArtifact));
        if (needsFrontend) {
            artifacts.add(frontendArtifact);
        }
        if (needsSchema) {
            artifacts.add(schemaArtifact);
        }
        artifactRepository.saveAll(artifacts);

        String backendUploadUrl = createUploadUrl(backendObjectKey, signatureDuration);
        ArtifactUploadResponse frontendUploadResponse = null;
        if (needsFrontend) {
            String frontendUploadUrl = createUploadUrl(frontendArtifact.getObjectKey(), signatureDuration);
            frontendUploadResponse = new ArtifactUploadResponse(frontendArtifact.getId(), frontendUploadUrl);
        }
        ArtifactUploadResponse schemaUploadResponse = null;
        if (needsSchema) {
            String schemaUploadUrl = createUploadUrl(schemaArtifact.getObjectKey(), signatureDuration);
            schemaUploadResponse = new ArtifactUploadResponse(schemaArtifact.getId(), schemaUploadUrl);
        }

        return new PresignedUrlResponse(
                new ArtifactUploadResponse(backendArtifact.getId(), backendUploadUrl),
                frontendUploadResponse,
                schemaUploadResponse,
                expiresAt
        );
    }

    private String validateFileName(String rawFileName, List<String> allowedExtensions, ErrorCode extensionError) {
        String fileName = rawFileName.trim();

        if (fileName.contains("/") || fileName.contains("\\") || fileName.indexOf('\0') >= 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME);
        }

        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        boolean matchesAny = allowedExtensions.stream().anyMatch(lowerFileName::endsWith);
        if (!matchesAny) {
            throw new BusinessException(extensionError);
        }

        return fileName;
    }

    private String createObjectKey(
            Long memberId,
            String requestId,
            String artifactDirectory,
            String fileName
    ) {
        return "artifacts/%d/%s/%s/%s".formatted(memberId, requestId, artifactDirectory, fileName);
    }

    private String createUploadUrl(String objectKey, Duration signatureDuration) {
        try {
            return presignedUrlProvider.createPutUrl(bucket, objectKey, signatureDuration);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }
}
