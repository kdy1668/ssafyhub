package com.ssafyhub.backend.domain.auth.controller;

import com.ssafyhub.backend.domain.auth.dto.LoginRequest;
import com.ssafyhub.backend.domain.auth.dto.LoginResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginResult;
import com.ssafyhub.backend.domain.auth.service.AuthService;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.login(request);

        cookieUtil.addAccessTokenCookie(response, result.accessToken());

        return ResponseEntity.ok(
                ApiResponse.success("로그인에 성공했습니다.", result.loginResponse())
        );
    }
}
