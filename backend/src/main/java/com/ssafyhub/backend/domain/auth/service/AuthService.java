package com.ssafyhub.backend.domain.auth.service;

import com.ssafyhub.backend.domain.auth.dto.LoginMemberResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginRequest;
import com.ssafyhub.backend.domain.auth.dto.LoginResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginResult;
import com.ssafyhub.backend.domain.auth.support.TokenProvider;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        Member member = memberRepository.findByStudentNumber(request.studentNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String role = member.getRole().name();
        String accessToken = tokenProvider.createAccessToken(member.getId(), role);

        LoginMemberResponse memberResponse = new LoginMemberResponse(
                member.getId(),
                member.getName(),
                member.getStudentNumber(),
                member.getTeamName(),
                role
        );

        return new LoginResult(accessToken, new LoginResponse(memberResponse));
    }
}
