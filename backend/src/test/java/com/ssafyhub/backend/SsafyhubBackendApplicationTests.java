package com.ssafyhub.backend;

import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.auth.repository.RefreshTokenRepository;
import com.ssafyhub.backend.domain.deployment.outbox.repository.DeploymentOutboxRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "jwt.secret=01234567890123456789012345678901",
        "security.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "deployment.outbox.enabled=false"
})
class SsafyhubBackendApplicationTests {

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private ArtifactRepository artifactRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private DeploymentRepository deploymentRepository;

    @MockitoBean
    private DeploymentEnvRepository deploymentEnvRepository;

    @MockitoBean
    private DeploymentLogRepository deploymentLogRepository;

    @MockitoBean
    private DeploymentOutboxRepository deploymentOutboxRepository;

    @Test
    void contextLoads() {
    }
}
