package com.ssafyhub.backend.domain.auth.service;

import com.ssafyhub.backend.domain.auth.dto.LoginMemberResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginRequest;
import com.ssafyhub.backend.domain.auth.dto.LoginResponse;
import com.ssafyhub.backend.domain.auth.dto.LoginResult;
import com.ssafyhub.backend.domain.auth.dto.ReissueResult;
import com.ssafyhub.backend.domain.auth.entity.RefreshToken;
import com.ssafyhub.backend.domain.auth.repository.RefreshTokenRepository;
import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final TokenHasher tokenHasher;

    @Transactional
    public LoginResult login(LoginRequest request) {
        CustomUserDetails userDetails = authenticate(request);

        Long memberId = userDetails.getMemberId();
        String role = userDetails.getRole();

        String accessToken = tokenProvider.createAccessToken(memberId, role);
        String refreshToken = tokenProvider.createRefreshToken(memberId);

        storeRefreshToken(memberRepository.getReferenceById(memberId), refreshToken);

        LoginMemberResponse memberResponse = new LoginMemberResponse(
                memberId,
                userDetails.getName(),
                userDetails.getStudentNumber(),
                userDetails.getTeamName(),
                role
        );

        return new LoginResult(accessToken, refreshToken, new LoginResponse(memberResponse));
    }

    private CustomUserDetails authenticate(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.studentNumber(),
                        request.password()
                );

        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            return (CustomUserDetails) authentication.getPrincipal();
        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
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
