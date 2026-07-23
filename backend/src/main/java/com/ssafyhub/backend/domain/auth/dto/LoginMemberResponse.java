package com.ssafyhub.backend.domain.auth.dto;

public record LoginMemberResponse(
        Long memberId,
        String name,
        String studentNumber,
        String teamName,
        String role
) {
}
