package com.ssafyhub.backend.domain.member.service;

import com.ssafyhub.backend.domain.member.dto.SignupRequest;
import com.ssafyhub.backend.domain.member.dto.MemberResponse;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_LONG);
        }

        String studentNumber = request.studentNumber();

        if (memberRepository.existsByStudentNumber(studentNumber)) {
            throw new BusinessException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Member member = new Member(studentNumber, passwordHash, request.name(), request.teamName());

        try {
            Member savedMember = memberRepository.save(member);
            return MemberResponse.from(savedMember);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
    }
}
