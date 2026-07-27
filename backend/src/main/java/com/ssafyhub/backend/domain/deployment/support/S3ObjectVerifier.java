package com.ssafyhub.backend.domain.deployment.support;

import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
public class S3ObjectVerifier {

    private final S3Client s3Client;

    public void verifyExists(String bucket, String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BusinessException(ErrorCode.ARTIFACT_FILE_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.ARTIFACT_STORAGE_CHECK_FAILED);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.ARTIFACT_STORAGE_CHECK_FAILED);
        }
    }
}
