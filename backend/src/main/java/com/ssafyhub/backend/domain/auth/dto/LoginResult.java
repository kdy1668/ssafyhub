package com.ssafyhub.backend.domain.auth.dto;

public record LoginResult(
        String accessToken,
        String refreshToken,
        LoginResponse loginResponse
) {
}
