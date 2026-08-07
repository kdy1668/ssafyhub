package com.ssafyhub.backend.domain.deployment.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeployGuideSourceRedactorTest {

    private final DeployGuideSourceRedactor redactor = new DeployGuideSourceRedactor();

    @Test
    @DisplayName("환경변수 이름은 보존하고 실제 비밀값만 가린다")
    void preservesEnvironmentKeysAndMasksLiteralSecrets() {
        String source = """
                password: ${DB_PASSWORD}
                client-secret: ${GOOGLE_CLIENT_SECRET:local-secret}
                jwt-secret: actual-secret-value
                apiKey: process.env.CULTURE_API_KEY
                """;

        String redacted = redactor.redact(source);

        assertThat(redacted)
                .contains("password: ${DB_PASSWORD}")
                .contains("client-secret: ${GOOGLE_CLIENT_SECRET:***}")
                .contains("jwt-secret: ***")
                .contains("apiKey: process.env.CULTURE_API_KEY")
                .doesNotContain("local-secret")
                .doesNotContain("actual-secret-value");
    }

    @Test
    @DisplayName("URL 자격증명과 AWS 접근 키는 소스에서도 가린다")
    void masksEmbeddedCredentials() {
        String source = """
                url: redis://admin:secret-password@redis:6379
                accessKey: AKIA1234567890ABCDEF
                """;

        assertThat(redactor.redact(source))
                .contains("redis://***:***@redis:6379")
                .doesNotContain("secret-password")
                .doesNotContain("AKIA1234567890ABCDEF");
    }
}
