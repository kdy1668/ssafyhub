package com.ssafyhub.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답. accessToken/refreshToken은 응답 바디가 아닌 Set-Cookie로 내려갑니다.")
public record LoginResponse(
        LoginMemberResponse member
) {
}
