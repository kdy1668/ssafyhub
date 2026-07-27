package com.ssafyhub.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafyhub.backend.domain.artifact.repository.ArtifactRepository;
import com.ssafyhub.backend.domain.auth.repository.RefreshTokenRepository;
import com.ssafyhub.backend.domain.deployment.outbox.repository.DeploymentOutboxRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentEnvRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentLogRepository;
import com.ssafyhub.backend.domain.deployment.repository.DeploymentRepository;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.domain.showcase.repository.ShowcaseDeploymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "jwt.secret=01234567890123456789012345678901",
        "security.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "deployment.outbox.enabled=false"
})
@AutoConfigureMockMvc
class SsafyhubBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

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
    private ShowcaseDeploymentRepository showcaseDeploymentRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void csrfEndpointIssuesReadableCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(cookie().secure("XSRF-TOKEN", false))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentNumber": "1234567",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_001"));
    }

    @Test
    void loginWithMismatchedCsrfTokenIsRejected() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentNumber": "1234567",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_001"));
    }

    @Test
    void loginWithCsrfCookieAndHeaderPassesCsrfValidation() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentNumber": "1234567",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }
}
