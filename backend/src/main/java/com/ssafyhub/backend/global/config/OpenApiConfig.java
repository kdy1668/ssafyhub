package com.ssafyhub.backend.global.config;

import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ACCESS_TOKEN_COOKIE_AUTH = "accessTokenCookieAuth";
    public static final String REFRESH_TOKEN_COOKIE_AUTH = "refreshTokenCookieAuth";
    public static final String CSRF_TOKEN_AUTH = "csrfTokenAuth";

    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN_COOKIE_AUTH, accessTokenCookieScheme())
                        .addSecuritySchemes(REFRESH_TOKEN_COOKIE_AUTH, refreshTokenCookieScheme())
                        .addSecuritySchemes(CSRF_TOKEN_AUTH, csrfTokenScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("SSAFYHUB Backend API")
                .description("""
                        SSAFYHUB 백엔드 API 명세서입니다.

                        인증은 로그인 시 발급되는 accessToken 쿠키로 이루어지며, \
                        POST/PATCH/DELETE 요청은 사전에 발급받은 CSRF 토큰(X-XSRF-TOKEN 헤더)을 함께 보내야 합니다. \
                        CSRF 토큰은 GET /api/v1/auth/csrf 로 발급받을 수 있습니다.""")
                .version("v1");
    }

    private SecurityScheme accessTokenCookieScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(JwtConstants.ACCESS_TOKEN_COOKIE_NAME)
                .description("로그인 시 발급되는 accessToken 쿠키. 인증이 필요한 API에 사용됩니다.");
    }

    private SecurityScheme refreshTokenCookieScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(JwtConstants.REFRESH_TOKEN_COOKIE_NAME)
                .description("로그인 시 발급되는 refreshToken 쿠키. 토큰 재발급 API에 사용됩니다.");
    }

    private SecurityScheme csrfTokenScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(CSRF_HEADER_NAME)
                .description("GET /api/v1/auth/csrf 로 발급받은 CSRF 토큰. 상태를 변경하는 모든 요청에 필요합니다.");
    }
}
