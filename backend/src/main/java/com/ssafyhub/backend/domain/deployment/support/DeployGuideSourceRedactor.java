package com.ssafyhub.backend.domain.deployment.support;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 배포 가이드에 첨부되는 프로젝트 소스에서 실제 비밀값만 가린다.
 *
 * <p>{@code ${DB_PASSWORD}}, {@code process.env.API_KEY} 같은 환경변수 참조는 배포에 필요한
 * 키를 찾는 증거이므로 보존한다. 반면 설정 파일에 직접 적힌 비밀번호, 토큰, 접근 키는 AI에
 * 전달하지 않는다. 일반 로그용 SecretRedactor와 목적이 달라 별도 클래스로 둔다.</p>
 */
@Component
public class DeployGuideSourceRedactor {

    private static final String MASK = "***";

    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)\\b([A-Za-z0-9_.\\[\\]-]*"
                    + "(?:password|passwd|pwd|secret|token|api[-_]?key|access[-_]?key|credential|authorization)"
                    + "[A-Za-z0-9_.-]*)"
                    + "(\\s*[=:]\\s*)(\\\"?)([^\\s\\\"',;)]+)\\3");

    private static final Pattern ENV_PLACEHOLDER = Pattern.compile(
            "\\$\\{([A-Za-z_][A-Za-z0-9_.-]*)(?::([^}]*))?}");

    private static final Pattern URL_CREDENTIAL =
            Pattern.compile("(?i)([a-z+]+://)([^/@\\s:]+):([^@\\s]+)@");

    private static final Pattern AWS_ACCESS_KEY_ID =
            Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");

    private static final Pattern JWT = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");

    private static final Pattern PEM_BLOCK =
            Pattern.compile("(?s)-----BEGIN[^-]+-----.*?-----END[^-]+-----");

    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = URL_CREDENTIAL.matcher(text).replaceAll("$1" + MASK + ":" + MASK + "@");
        result = redactSecretAssignments(result);
        result = AWS_ACCESS_KEY_ID.matcher(result).replaceAll(MASK);
        result = JWT.matcher(result).replaceAll(MASK);
        result = PEM_BLOCK.matcher(result).replaceAll(MASK);
        return result;
    }

    private String redactSecretAssignments(String text) {
        Matcher matcher = KEY_VALUE_SECRET.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String value = matcher.group(4);
            String replacementValue = isEnvironmentReference(value)
                    ? preserveKeyAndMaskDefault(value)
                    : MASK;
            String replacement = matcher.group(1) + matcher.group(2)
                    + matcher.group(3) + replacementValue + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private boolean isEnvironmentReference(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return ENV_PLACEHOLDER.matcher(value).matches()
                || normalized.startsWith("process.env.")
                || normalized.startsWith("import.meta.env.")
                || normalized.startsWith("os.environ")
                || normalized.startsWith("system.getenv");
    }

    private String preserveKeyAndMaskDefault(String value) {
        Matcher matcher = ENV_PLACEHOLDER.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        return matcher.group(2) == null
                ? "${" + matcher.group(1) + "}"
                : "${" + matcher.group(1) + ":" + MASK + "}";
    }
}
