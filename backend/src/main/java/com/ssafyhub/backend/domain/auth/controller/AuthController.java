package com.ssafyhub.backend.domain.auth.controller;

import com.ssafyhub.backend.domain.auth.dto.LoginRequest;
import com.ssafyhub.backend.domain.auth.dto.LoginResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginResult;
import com.ssafyhub.backend.domain.auth.dto.ReissueResult;
import com.ssafyhub.backend.domain.auth.service.AuthService;
import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import com.ssafyhub.backend.global.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Void>> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity.ok(
                ApiResponse.success("CSRF 토큰이 발급되었습니다.", null)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.login(request);

        cookieUtil.addAccessTokenCookie(response, result.accessToken());
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("로그인에 성공했습니다.", result.loginResponse())
        );
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);

        ReissueResult result = authService.reissue(refreshToken);

        cookieUtil.addAccessTokenCookie(response, result.accessToken());
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("토큰이 재발급되었습니다.", null)
        );
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }

        for (Cookie cookie : cookies) {
            if (JwtConstants.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
    }
}
