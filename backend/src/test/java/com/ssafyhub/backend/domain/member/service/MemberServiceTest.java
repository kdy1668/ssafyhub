package com.ssafyhub.backend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssafyhub.backend.domain.member.dto.SignupRequest;
import com.ssafyhub.backend.domain.member.dto.MemberResponse;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private SignupRequest validRequest(String studentNumber) {
        return new SignupRequest("team-a", "홍길동", studentNumber, "password1234", "password1234");
    }

    @Test
    void 정상_가입이면_학번을_저장한다() {
        SignupRequest request = validRequest("1551343");
        given(memberRepository.existsByStudentNumber("1551343")).willReturn(false);
        given(passwordEncoder.encode("password1234")).willReturn("encodedHash");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        MemberResponse response = memberService.signup(request);

        assertThat(response.studentNumber()).isEqualTo("1551343");
        verify(memberRepository).existsByStudentNumber("1551343");
    }

    @Test
    void 학번이_이미_존재하면_학번_중복_예외가_발생한다() {
        SignupRequest request = validRequest("1551343");
        given(memberRepository.existsByStudentNumber("1551343")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_STUDENT_NUMBER);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void 동시_가입으로_유니크_제약을_위반하면_학번_중복으로_변환한다() {
        SignupRequest request = validRequest("1551343");
        given(memberRepository.existsByStudentNumber("1551343")).willReturn(false);
        given(passwordEncoder.encode("password1234")).willReturn("encodedHash");
        given(memberRepository.save(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_STUDENT_NUMBER);
    }

    @Test
    void 비밀번호와_비밀번호_확인이_다르면_예외가_발생한다() {
        SignupRequest request = new SignupRequest("team-a", "홍길동", "1551343", "password1234", "different1234");

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH);

        verify(memberRepository, never()).existsByStudentNumber(any());
    }

    @Test
    void 비밀번호가_72바이트를_초과하면_예외가_발생한다() {
        String longPassword = "가".repeat(30);
        SignupRequest request = new SignupRequest("team-a", "홍길동", "1551343", longPassword, longPassword);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_TOO_LONG);

        verify(memberRepository, never()).existsByStudentNumber(any());
    }

    @Test
    void 비밀번호는_해시로_저장된다() {
        SignupRequest request = validRequest("1551343");
        given(memberRepository.existsByStudentNumber("1551343")).willReturn(false);
        given(passwordEncoder.encode("password1234")).willReturn("encodedHash");
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        given(memberRepository.save(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        memberService.signup(request);

        assertThat(captor.getValue().getPasswordHash())
                .isEqualTo("encodedHash")
                .isNotEqualTo("password1234");
    }
}
