package com.ssafyhub.backend.domain.auth.dto;

public record ReissueResult(
        String accessToken,
        String refreshToken
) {
}
