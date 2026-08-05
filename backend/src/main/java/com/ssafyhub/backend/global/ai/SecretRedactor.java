package com.ssafyhub.backend.global.ai;

import java.util.Collection;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * LLM에 보낼 로그에서 비밀값을 치환한다.
 *
 * 이 클래스는 2차 방어선이다. 1차 방어선은 "복호화하지 않는 것"이다 -
 * deployment_envs.encrypted_value와 encrypted_database_password는 프롬프트를 만들 때
 * 복호화하지 않고 키 이름만 쓴다. 여기서 막는 것은 사용자 앱이나 빌드 도구가
 * 자기 비밀값을 로그로 출력해버린 경우다.
 */
@Component
public class SecretRedactor {

    private static final String MASK = "***";

    /** 정확 일치 치환에서 제외할 최소 길이. 짧은 값은 오탐이 많아 로그를 망친다. */
    private static final int MIN_EXACT_MATCH_LENGTH = 5;

    // R1: URL에 박힌 자격증명 (jdbc:mysql://user:pass@host, redis://user:pass@host 등)
    private static final Pattern URL_CREDENTIAL =
            Pattern.compile("(?i)([a-z+]+://)([^/@\\s:]+):([^@\\s]+)@");

    // R2: 키=값 / 키: 값 형태의 비밀. 값에 따옴표가 있으면 그것까지 포함해 치환한다.
    //
    // 비밀 단어 앞뒤로 식별자 문자를 허용하는 것이 중요하다. \b(secret)\b로 두면
    // JWT_SECRET / DB_PASSWORD / MY_API_KEY가 하나도 걸리지 않는다 - '_'가 단어 문자라서
    // 'T_SECRET' 사이에 단어 경계가 없기 때문이다. 실제 환경변수 이름은 대부분 이 형태다.
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)\\b([A-Za-z0-9_.\\[\\]-]*"
                    + "(?:password|passwd|pwd|secret|token|api[-_]?key|access[-_]?key|credential|authorization)"
                    + "[A-Za-z0-9_.-]*)"
                    + "(\\s*[=:]\\s*)(\"?)([^\\s\"',;)]+)\\3");

    // R3: HTTP Authorization 헤더 (Bearer 접두어 유무 모두)
    private static final Pattern AUTHORIZATION_HEADER =
            Pattern.compile("(?i)(authorization\\s*:\\s*)(?:bearer\\s+)?\\S+");

    // R4: AWS 액세스 키 ID
    private static final Pattern AWS_ACCESS_KEY_ID =
            Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");

    // R5: JWT (header.payload.signature)
    private static final Pattern JWT = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");

    // R6: PEM 블록 (개인키가 로그로 나오는 경우)
    private static final Pattern PEM_BLOCK =
            Pattern.compile("(?s)-----BEGIN[^-]+-----.*?-----END[^-]+-----");

    /** 패턴 기반 치환만 수행한다. */
    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        result = URL_CREDENTIAL.matcher(result).replaceAll("$1" + MASK + ":" + MASK + "@");
        // AUTHORIZATION_HEADER가 KEY_VALUE_SECRET보다 먼저 돌아야 한다.
        // 순서를 바꾸면 "Authorization: Bearer <토큰>"에서 R2가 값으로 "Bearer"만 먹고 끝나
        // 정작 토큰이 그대로 남는다("Authorization: *** <토큰>").
        result = AUTHORIZATION_HEADER.matcher(result).replaceAll("$1" + MASK);
        result = KEY_VALUE_SECRET.matcher(result).replaceAll("$1$2" + MASK);
        result = AWS_ACCESS_KEY_ID.matcher(result).replaceAll(MASK);
        result = JWT.matcher(result).replaceAll(MASK);
        result = PEM_BLOCK.matcher(result).replaceAll(MASK);
        return result;
    }

    /**
     * 패턴 치환 + 알려진 비밀값의 정확 일치 치환.
     *
     * 패턴 치환은 형식 없이 값만 찍힌 경우(예: 로그에 비밀번호 문자열만 출력)를 놓친다.
     * 호출부가 알고 있는 비밀값 목록을 넘기면 그 값을 정확 일치로 함께 지운다.
     *
     * 주의: 넘긴 값은 치환에만 쓰이고 프롬프트·로그·응답에 절대 포함되지 않는다.
     * 호출부는 이 목록을 지역 변수로만 다루고 필드나 캐시에 보관하지 않아야 한다.
     */
    public String redact(String text, Collection<String> knownSecrets) {
        String result = redact(text);
        if (result == null || knownSecrets == null || knownSecrets.isEmpty()) {
            return result;
        }

        for (String secret : knownSecrets) {
            if (secret == null || secret.length() < MIN_EXACT_MATCH_LENGTH) {
                continue;
            }
            result = result.replace(secret, MASK);
        }
        return result;
    }
}
