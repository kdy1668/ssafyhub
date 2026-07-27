package com.ssafyhub.backend.domain.auth.support;

public final class JwtConstants {

    public static final String ROLE_CLAIM = "role";
    public static final String TOKEN_TYPE_CLAIM = "tokenType";

    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private JwtConstants() {
    }
}
