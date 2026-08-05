package com.ssafyhub.backend.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 마스킹 규칙 R1~R6 검증. 요구사항 FR-1.6 / NFR-3.2 대응.
 *
 * "치환됐는지"만 보지 않고 "원본 비밀값이 결과에 남아 있지 않은지"를 함께 확인한다.
 * 부분 치환은 통과처럼 보이면서 실제로는 값이 새는 경우가 있다.
 */
class SecretRedactorTest {

    private final SecretRedactor redactor = new SecretRedactor();

    @Test
    @DisplayName("R1 - JDBC URL의 자격증명을 치환한다")
    void redactsJdbcUrlCredentials() {
        String log = "jdbc:mysql://appuser:s3cr3tP@ss@mysql:3306/mydb 연결 시도";

        String result = redactor.redact(log);

        assertThat(result).doesNotContain("appuser").doesNotContain("s3cr3tP");
        assertThat(result).contains("jdbc:mysql://***:***@");
    }

    @Test
    @DisplayName("R2 - 키=값 형태의 비밀을 치환한다")
    void redactsKeyValueSecrets() {
        String log = """
                spring.datasource.password=hunter2
                JWT_SECRET: my-very-long-secret-value
                api_key = ak_live_9182734
                """;

        String result = redactor.redact(log);

        assertThat(result).doesNotContain("hunter2");
        assertThat(result).doesNotContain("my-very-long-secret-value");
        assertThat(result).doesNotContain("ak_live_9182734");
        // 키 이름은 남아야 한다 - 어떤 설정이 문제인지 AI가 알아야 진단이 된다.
        assertThat(result).contains("password=***");
        assertThat(result).contains("JWT_SECRET: ***");
    }

    @Test
    @DisplayName("R3 - Authorization 헤더를 Bearer 유무와 무관하게 치환한다")
    void redactsAuthorizationHeader() {
        String log = """
                Authorization: Bearer abcdef123456
                authorization:xyz789raw
                """;

        String result = redactor.redact(log);

        assertThat(result).doesNotContain("abcdef123456").doesNotContain("xyz789raw");
    }

    @Test
    @DisplayName("R4 - AWS 액세스 키 ID를 치환한다")
    void redactsAwsAccessKeyId() {
        String log = "using credentials AKIAIOSFODNN7EXAMPLE for bucket access";

        String result = redactor.redact(log);

        assertThat(result).doesNotContain("AKIAIOSFODNN7EXAMPLE").contains("***");
    }

    @Test
    @DisplayName("R5 - JWT를 치환한다")
    void redactsJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dBjftJeZ4CVPmB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String log = "token issued: " + jwt;

        String result = redactor.redact(log);

        assertThat(result).doesNotContain(jwt).contains("***");
    }

    @Test
    @DisplayName("R6 - PEM 블록을 치환한다")
    void redactsPemBlock() {
        String log = """
                -----BEGIN RSA PRIVATE KEY-----
                MIIEowIBAAKCAQEAxGZ1
                -----END RSA PRIVATE KEY-----
                """;

        String result = redactor.redact(log);

        assertThat(result).doesNotContain("MIIEowIBAAKCAQEAxGZ1").contains("***");
    }

    @Test
    @DisplayName("비밀이 아닌 로그는 그대로 둔다")
    void keepsOrdinaryLogIntact() {
        String log = """
                Started Application in 6.2 seconds
                Caused by: com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
                Tomcat started on port 8080
                """;

        assertThat(redactor.redact(log)).isEqualTo(log);
    }

    @Test
    @DisplayName("알려진 비밀값은 형식 없이 찍혀도 정확 일치로 치환한다")
    void redactsKnownSecretsByExactMatch() {
        // 사용자 앱이 비밀번호를 아무 형식 없이 그대로 출력한 경우 - 패턴만으로는 잡히지 않는다.
        String log = "DB 접속 실패. 사용한 값: myTopSecretPw123";

        String patternOnly = redactor.redact(log);
        assertThat(patternOnly).contains("myTopSecretPw123");

        String result = redactor.redact(log, List.of("myTopSecretPw123"));
        assertThat(result).doesNotContain("myTopSecretPw123").contains("***");
    }

    @Test
    @DisplayName("짧은 비밀값은 정확 일치 대상에서 제외한다")
    void skipsShortKnownSecrets() {
        // 4자 이하 값까지 치환하면 평범한 단어가 뭉개져 로그를 못 읽게 된다.
        String log = "port 8080 에서 기동";

        String result = redactor.redact(log, List.of("8080"));

        assertThat(result).contains("8080");
    }

    @Test
    @DisplayName("null과 빈 문자열을 안전하게 처리한다")
    void handlesNullAndEmpty() {
        assertThat(redactor.redact(null)).isNull();
        assertThat(redactor.redact("")).isEmpty();
        assertThat(redactor.redact(null, List.of("x"))).isNull();
        assertThat(redactor.redact("abc", null)).isEqualTo("abc");
    }
}
