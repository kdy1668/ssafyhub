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

        String backendFileName = validateFileName(request.backendFileName(), ".jar", ErrorCode.INVALID_BACKEND_FILE);
        String frontendFileName = validateFileName(request.frontendFileName(), ".zip", ErrorCode.INVALID_FRONTEND_FILE);

        String requestId = UUID.randomUUID().toString();
        String backendObjectKey = createObjectKey(memberId, requestId, "backend", backendFileName);
        String frontendObjectKey = createObjectKey(memberId, requestId, "frontend", frontendFileName);
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
        Artifact frontendArtifact = new Artifact(
                member,
                ArtifactType.FRONTEND_ZIP,
                frontendFileName,
                bucket,
                frontendObjectKey,
                expiresAt
        );

        artifactRepository.saveAll(List.of(backendArtifact, frontendArtifact));

        String backendUploadUrl = createUploadUrl(backendObjectKey, signatureDuration);
        String frontendUploadUrl = createUploadUrl(frontendObjectKey, signatureDuration);

        return new PresignedUrlResponse(
                new ArtifactUploadResponse(backendArtifact.getId(), backendUploadUrl),
                new ArtifactUploadResponse(frontendArtifact.getId(), frontendUploadUrl),
                expiresAt
        );
    }

    private String validateFileName(String rawFileName, String requiredExtension, ErrorCode extensionError) {
        String fileName = rawFileName.trim();

        if (fileName.contains("/") || fileName.contains("\\") || fileName.indexOf('\0') >= 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE_NAME);
        }

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(requiredExtension)) {
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
