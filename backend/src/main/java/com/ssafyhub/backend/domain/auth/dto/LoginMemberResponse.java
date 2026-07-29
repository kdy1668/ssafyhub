package com.ssafyhub.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인한 회원 정보")
public record LoginMemberResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "학번", example = "1234567")
        String studentNumber,

        @Schema(description = "팀명", example = "SSAFYHUB")
        String teamName,

        @Schema(description = "권한", example = "MEMBER")
        String role
) {
}
