package com.ssafyhub.backend.global.util;

import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

    private final boolean secure;

    public CookieUtil(@Value("${security.cookie.secure}") boolean secure) {
        this.secure = secure;
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = ResponseCookie.from(JwtConstants.ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(secure)
                .path(ACCESS_TOKEN_COOKIE_PATH)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
