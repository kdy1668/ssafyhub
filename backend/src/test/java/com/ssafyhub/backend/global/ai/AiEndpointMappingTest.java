package com.ssafyhub.backend.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.auth.repository.RefreshTokenRepository;
import com.ssafyhub.backend.domain.deployment.outbox.repository.DeploymentOutboxRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentDiagnosisRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * AI 기능을 켠 상태로 전체 컨텍스트를 띄워 URL 매핑을 검증한다.
 *
 * 이 테스트가 필요한 이유가 명확하다. AI 엔드포인트는 기존 DeploymentController를 고치지 않기
 * 위해 별도 컨트롤러로 분리했고, 그 경로가 기존 매핑과 겹치지 않는다는 전제 위에 있다.
 * 겹치면 Spring이 기동 시점에 IllegalStateException(Ambiguous mapping)으로 죽는다.
 * 즉 이 테스트가 통과한다는 것 자체가 "충돌 없음"의 증거다.
 *
 * 기존 SsafyhubBackendApplicationTests와 설정이 비슷하지만 그 파일을 건드리지 않기 위해
 * 별도로 둔다. 그 테스트는 ai.enabled를 켜지 않으므로 이 조합을 검증하지 못한다.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "jwt.secret=01234567890123456789012345678901",
        "security.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "deployment.outbox.enabled=false",
        // AI를 켜고 띄운다. 키가 있어야 클라이언트 빈까지 생성되어 조립 전체가 검증된다.
        "ai.enabled=true",
        "ai.api-key=test-key-not-used-for-real-calls"
})
@AutoConfigureMockMvc
class AiEndpointMappingTest {

    @Autowired
    private MockMvc mockMvc;

    // RequestMappingHandlerMapping 빈이 둘이다(MVC 기본 + springdoc). 기본 것을 지정한다.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

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
    @MockitoBean
    private DeploymentDiagnosisRepository deploymentDiagnosisRepository;

    @Test
    @DisplayName("AI를 켠 상태로 컨텍스트가 뜬다 (기존 매핑과 충돌 없음)")
    void contextLoadsWithAiEnabled() {
        assertThat(handlerMapping).isNotNull();
    }

    @Test
    @DisplayName("AI 엔드포인트 3개가 모두 매핑된다")
    void registersAiEndpoints() {
        var patterns = handlerMapping.getHandlerMethods().keySet().stream()
                .map(RequestMappingInfo::toString)
                .toList();

        assertThat(patterns).anySatisfy(pattern -> assertThat(pattern)
                .contains("/api/v1/ai/availability").contains("GET"));
        assertThat(patterns).anySatisfy(pattern -> assertThat(pattern)
                .contains("/api/v1/deployments/{deploymentId}/diagnosis").contains("POST"));
        assertThat(patterns).anySatisfy(pattern -> assertThat(pattern)
                .contains("/api/v1/deployments/{deploymentId}/runtime-logs/detection")
                .contains("POST"));
    }

    @Test
    @DisplayName("기존 배포 API 매핑이 그대로 남아 있다")
    void keepsExistingDeploymentEndpoints() {
        var patterns = handlerMapping.getHandlerMethods().keySet().stream()
                .map(RequestMappingInfo::toString)
                .toList();

        // 새 컨트롤러가 기존 경로를 가로채지 않았는지 확인한다.
        assertThat(patterns).anySatisfy(pattern -> assertThat(pattern)
                .contains("/api/v1/deployments/{deploymentId}/logs").contains("GET"));
        assertThat(patterns).anySatisfy(pattern -> assertThat(pattern)
                .contains("/api/v1/deployments/{deploymentId}/runtime-logs").contains("GET"));
        assertThat(patterns).anySatisfy(pattern -> assertThat(pattern)
                .contains("/api/v1/deployments/{deploymentId}").contains("DELETE"));
    }

    /**
     * 아래 테스트들은 "매핑이 존재한다"가 아니라 "보호되어 있다"를 검증한다.
     * Spring Security 필터가 디스패처보다 먼저 돌기 때문에 없는 경로도 401/403이 되므로,
     * 상태 코드만으로는 매핑 존재를 증명할 수 없다. 매핑 존재는 위의 handlerMapping 검사가 담당한다.
     *
     * 두 POST 엔드포인트가 401이 아니라 403(CSRF_001)인 것이 정상이다. CSRF 필터가 인증
     * 필터보다 먼저 돌기 때문이며, 상태 변경 요청에 기존 CSRF 정책이 그대로 적용되고 있다는
     * 뜻이다(NFR-3.5). 기존 로그인 API도 같은 방식으로 동작한다.
     */
    @Test
    @DisplayName("진단 엔드포인트에 기존 CSRF 정책이 적용된다")
    void diagnosisIsCsrfProtected() throws Exception {
        mockMvc.perform(post("/api/v1/deployments/1/diagnosis"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_001"));
    }

    @Test
    @DisplayName("감지 엔드포인트에 기존 CSRF 정책이 적용된다")
    void detectionIsCsrfProtected() throws Exception {
        mockMvc.perform(post("/api/v1/deployments/1/runtime-logs/detection"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_001"));
    }

    @Test
    @DisplayName("availability는 인증이 필요하다 (CSRF 대상 아님)")
    void availabilityRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/ai/availability"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
