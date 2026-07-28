package com.ssafyhub.backend.domain.member.dto;

import com.ssafyhub.backend.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "회원 정보 응답")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1")
        Long id,

        @Schema(description = "학번", example = "1234567")
        String studentNumber,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "팀명", example = "SSAFYHUB")
        String teamName,

        @Schema(description = "가입 일시", example = "2026-07-28T12:00:00")
        LocalDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getStudentNumber(),
                member.getName(),
                member.getTeamName(),
                member.getCreatedAt()
        );
    }
}
