package com.ssafyhub.backend.domain.auth.service;

import com.ssafyhub.backend.domain.auth.dto.LoginMemberResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginRequest;
import com.ssafyhub.backend.domain.auth.dto.LoginResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginResult;
import com.ssafyhub.backend.domain.auth.dto.ReissueResult;
import com.ssafyhub.backend.domain.auth.entity.RefreshToken;
import com.ssafyhub.backend.domain.auth.repository.RefreshTokenRepository;
import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import com.ssafyhub.backend.domain.auth.support.TokenHasher;
import com.ssafyhub.backend.domain.auth.support.TokenProvider;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;

    @Transactional
    public LoginResult login(LoginRequest request) {
        Member member = memberRepository.findByStudentNumber(request.studentNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String role = member.getRole().name();
        String accessToken = tokenProvider.createAccessToken(member.getId(), role);
        String refreshToken = tokenProvider.createRefreshToken(member.getId());

        storeRefreshToken(member, refreshToken);

        LoginMemberResponse memberResponse = new LoginMemberResponse(
                member.getId(),
                member.getName(),
                member.getStudentNumber(),
                member.getTeamName(),
                role
        );

        return new LoginResult(accessToken, refreshToken, new LoginResponse(memberResponse));
    }

    @Transactional
    public ReissueResult reissue(String refreshToken) {
        Claims claims = tokenProvider.parseClaims(refreshToken);

        if (!JwtConstants.REFRESH_TOKEN_TYPE.equals(tokenProvider.getTokenType(claims))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String tokenHash = tokenHasher.hash(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.isRevoked()) {
            throw new BusinessException(ErrorCode.REVOKED_REFRESH_TOKEN);
        }

        if (storedToken.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        Long memberId = tokenProvider.getMemberId(claims);
        if (!storedToken.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        storedToken.revoke();

        String role = member.getRole().name();
        String newAccessToken = tokenProvider.createAccessToken(member.getId(), role);
        String newRefreshToken = tokenProvider.createRefreshToken(member.getId());

        storeRefreshToken(member, newRefreshToken);

        return new ReissueResult(newAccessToken, newRefreshToken);
    }

    private void storeRefreshToken(Member member, String refreshToken) {
        String tokenHash = tokenHasher.hash(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMillis()));

        refreshTokenRepository.save(new RefreshToken(member, tokenHash, expiresAt));
    }
}
