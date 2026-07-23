package com.ssafyhub.backend.domain.member.dto;

import com.ssafyhub.backend.domain.member.entity.Member;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String studentNumber,
        String name,
        String teamName,
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
