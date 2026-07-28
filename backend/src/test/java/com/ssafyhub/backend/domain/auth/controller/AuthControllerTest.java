package com.ssafyhub.backend.domain.auth.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafyhub.backend.domain.auth.dto.ReissueResult;
import com.ssafyhub.backend.domain.auth.service.AuthService;
import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import com.ssafyhub.backend.global.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

class AuthControllerTest {

    @Test
    void reissueMaterializesNewCsrfToken() {
        AuthService authService = mock(AuthService.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        CsrfToken csrfToken = mock(CsrfToken.class);
        AuthController controller = new AuthController(authService, cookieUtil);

        String refreshToken = "refresh-token";
        ReissueResult result = new ReissueResult("new-access-token", "new-refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
        });
        when(authService.reissue(refreshToken)).thenReturn(result);

        controller.reissue(request, response, csrfToken);

        verify(cookieUtil).addAccessTokenCookie(response, result.accessToken());
        verify(cookieUtil).addRefreshTokenCookie(response, result.refreshToken());
        verify(csrfToken).getToken();
    }
}
