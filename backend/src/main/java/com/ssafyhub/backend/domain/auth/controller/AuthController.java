package com.ssafyhub.backend.domain.auth.controller;

import com.ssafyhub.backend.domain.auth.dto.LoginRequest;
import com.ssafyhub.backend.domain.auth.dto.LoginResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginResult;
import com.ssafyhub.backend.domain.auth.dto.ReissueResult;
import com.ssafyhub.backend.domain.auth.service.AuthService;
import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.config.OpenApiConfig;
import com.ssafyhub.backend.global.config.swagger.ApiErrorCodeExample;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import com.ssafyhub.backend.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Operation(
            summary = "CSRF 토큰 발급",
            description = "상태를 변경하는 요청(POST/PATCH/DELETE) 전에 호출합니다. "
                    + "응답 Set-Cookie로 XSRF-TOKEN 쿠키가 내려가며, 이후 요청 시 그 값을 X-XSRF-TOKEN 헤더에 담아 보내야 합니다."
    )
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Void>> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity.ok(
                ApiResponse.success("CSRF 토큰이 발급되었습니다.", null)
        );
    }

    @Operation(
            summary = "로그인",
            description = "학번/비밀번호로 로그인합니다. 응답 Set-Cookie로 accessToken/refreshToken 쿠키가 내려갑니다."
    )
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.VALIDATION_FAILED)
    @ApiErrorCodeExample(ErrorCode.LOGIN_FAILED)
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

    @Operation(
            summary = "토큰 재발급",
            description = "refreshToken 쿠키를 검증해 accessToken/refreshToken을 재발급합니다."
    )
    @SecurityRequirement(name = OpenApiConfig.REFRESH_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @ApiErrorCodeExample(ErrorCode.MISSING_REFRESH_TOKEN)
    @ApiErrorCodeExample(ErrorCode.INVALID_REFRESH_TOKEN)
    @ApiErrorCodeExample(ErrorCode.EXPIRED_REFRESH_TOKEN)
    @ApiErrorCodeExample(ErrorCode.REVOKED_REFRESH_TOKEN)
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(
            HttpServletRequest request,
            HttpServletResponse response,
            CsrfToken csrfToken
    ) {
        String refreshToken = extractRefreshToken(request);

        ReissueResult result = authService.reissue(refreshToken);

        cookieUtil.addAccessTokenCookie(response, result.accessToken());
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken());
        csrfToken.getToken();

        return ResponseEntity.ok(
                ApiResponse.success("토큰이 재발급되었습니다.", null)
        );
    }

    @Operation(
            summary = "로그아웃",
            description = "refreshToken을 폐기하고 accessToken/refreshToken 쿠키를 삭제합니다. "
                    + "쿠키가 없거나 이미 만료·폐기된 상태에서도 항상 성공(200)으로 응답합니다. "
                    + "accessToken은 stateless JWT이므로 남은 유효기간 동안 토큰 자체는 무효화되지 않습니다."
    )
    @SecurityRequirement(name = OpenApiConfig.REFRESH_TOKEN_COOKIE_AUTH)
    @SecurityRequirement(name = OpenApiConfig.CSRF_TOKEN_AUTH)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(findRefreshToken(request));

        // 폐기 결과와 무관하게 쿠키는 항상 삭제한다.
        cookieUtil.deleteAccessTokenCookie(response);
        cookieUtil.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok(
                ApiResponse.success("로그아웃되었습니다.", null)
        );
    }

    private String extractRefreshToken(HttpServletRequest request) {
        String refreshToken = findRefreshToken(request);

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }

        return refreshToken;
    }

    private String findRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (JwtConstants.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
