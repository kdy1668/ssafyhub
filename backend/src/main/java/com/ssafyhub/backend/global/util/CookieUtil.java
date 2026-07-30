package com.ssafyhub.backend.global.util;

import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

    // 음수 Max-Age는 브라우저 세션 쿠키(만료 속성 없음)를 의미한다.
    private static final Duration SESSION_MAX_AGE = Duration.ofSeconds(-1);
    private static final Duration DELETE_MAX_AGE = Duration.ZERO;

    private final boolean secure;

    public CookieUtil(@Value("${security.cookie.secure}") boolean secure) {
        this.secure = secure;
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        setCookie(
                response,
                JwtConstants.ACCESS_TOKEN_COOKIE_NAME,
                accessToken,
                ACCESS_TOKEN_COOKIE_PATH,
                SESSION_MAX_AGE
        );
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setCookie(
                response,
                JwtConstants.REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                REFRESH_TOKEN_COOKIE_PATH,
                SESSION_MAX_AGE
        );
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        setCookie(response, JwtConstants.ACCESS_TOKEN_COOKIE_NAME, "", ACCESS_TOKEN_COOKIE_PATH, DELETE_MAX_AGE);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        setCookie(response, JwtConstants.REFRESH_TOKEN_COOKIE_NAME, "", REFRESH_TOKEN_COOKIE_PATH, DELETE_MAX_AGE);
    }

    // 발급과 삭제가 같은 경로/속성을 쓰도록 한곳에서 쿠키를 만든다.
    // 경로가 어긋나면 브라우저는 다른 쿠키로 보고 삭제하지 않는다.
    private void setCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path,
            Duration maxAge
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
