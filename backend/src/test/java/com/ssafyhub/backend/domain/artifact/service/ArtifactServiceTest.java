package com.ssafyhub.backend.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlRequest;
import com.ssafyhub.backend.domain.artifact.dto.PresignedUrlResponse;
import com.ssafyhub.backend.domain.artifact.entity.Artifact;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactStatus;
import com.ssafyhub.backend.domain.artifact.enums.ArtifactType;
import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.artifact.support.S3PresignedUrlProvider;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ArtifactRepository artifactRepository;

    @Mock
    private S3PresignedUrlProvider presignedUrlProvider;

    @InjectMocks
    private ArtifactService artifactService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(artifactService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(artifactService, "expirationMinutes", 10L);
    }

    @Test
    void JAR과_ZIP의_업로드_URL을_발급하고_메타데이터를_저장한다() {
        Member member = new Member("1551343", "hash", "홍길동", "E206");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(artifactRepository.saveAll(any())).willAnswer(invocation -> {
            List<Artifact> artifacts = invocation.getArgument(0);
            AtomicLong id = new AtomicLong(10L);
            artifacts.forEach(artifact -> ReflectionTestUtils.setField(artifact, "id", id.getAndIncrement()));
            return artifacts;
        });
        given(presignedUrlProvider.createPutUrl(anyString(), anyString(), any(Duration.class)))
                .willReturn("https://s3.example/backend", "https://s3.example/frontend");

        PresignedUrlResponse response = artifactService.issuePresignedUrls(
                1L,
                new PresignedUrlRequest(" app.JAR ", " dist.ZIP ")
        );

        assertThat(response.backendArtifact().artifactId()).isEqualTo(10L);
        assertThat(response.backendArtifact().uploadUrl()).isEqualTo("https://s3.example/backend");
        assertThat(response.frontendArtifact().artifactId()).isEqualTo(11L);
        assertThat(response.frontendArtifact().uploadUrl()).isEqualTo("https://s3.example/frontend");

        ArgumentCaptor<List<Artifact>> captor = ArgumentCaptor.forClass(List.class);
        verify(artifactRepository).saveAll(captor.capture());
        List<Artifact> artifacts = captor.getValue();

        assertThat(artifacts)
                .extracting(Artifact::getArtifactType)
                .containsExactly(ArtifactType.BACKEND_JAR, ArtifactType.FRONTEND_ZIP);
        assertThat(artifacts)
                .extracting(Artifact::getStatus)
                .containsOnly(ArtifactStatus.URL_ISSUED);
        assertThat(artifacts)
                .extracting(Artifact::getBucket)
                .containsOnly("test-bucket");
        assertThat(artifacts.get(0).getObjectKey()).contains("artifacts/1/").endsWith("/backend/app.JAR");
        assertThat(artifacts.get(1).getObjectKey()).contains("artifacts/1/").endsWith("/frontend/dist.ZIP");
        assertThat(artifacts.get(0).getExpiresAt()).isEqualTo(artifacts.get(1).getExpiresAt());
    }

    @Test
    void 백엔드_파일이_JAR이_아니면_거부한다() {
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(new Member("1551343", "hash", "홍길동", "E206")));

        assertThatThrownBy(() -> artifactService.issuePresignedUrls(
                1L,
                new PresignedUrlRequest("app.zip", "dist.zip")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_BACKEND_FILE);

        verify(artifactRepository, never()).saveAll(any());
    }

    @Test
    void 프론트엔드_파일이_ZIP이_아니면_거부한다() {
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(new Member("1551343", "hash", "홍길동", "E206")));

        assertThatThrownBy(() -> artifactService.issuePresignedUrls(
                1L,
                new PresignedUrlRequest("app.jar", "dist.jar")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FRONTEND_FILE);
    }

    @Test
    void 경로가_포함된_파일_이름은_거부한다() {
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(new Member("1551343", "hash", "홍길동", "E206")));

        assertThatThrownBy(() -> artifactService.issuePresignedUrls(
                1L,
                new PresignedUrlRequest("../app.jar", "dist.zip")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_NAME);
    }

    @Test
    void 회원이_없으면_발급하지_않는다() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> artifactService.issuePresignedUrls(
                1L,
                new PresignedUrlRequest("app.jar", "dist.zip")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verify(presignedUrlProvider, never()).createPutUrl(anyString(), anyString(), any(Duration.class));
    }
}
