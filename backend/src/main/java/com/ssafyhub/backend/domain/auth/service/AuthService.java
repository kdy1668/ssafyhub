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

    /**
     * 전달된 Refresh Token을 폐기한다. 이후 재발급은 REVOKED_REFRESH_TOKEN으로 거부된다.
     *
     * <p>로그아웃은 멱등해야 하므로 쿠키가 없거나(null) 이미 폐기·만료된 토큰이어도 예외를 던지지 않는다.
     * 여기서 401을 던지면 클라이언트가 쿠키를 정리할 기회를 잃는다.
     *
     * <p>같은 회원의 다른 Refresh Token은 건드리지 않는다(기기별 로그아웃).
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        refreshTokenRepository.findByTokenHash(tokenHasher.hash(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private void storeRefreshToken(Member member, String refreshToken) {
        String tokenHash = tokenHasher.hash(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMillis()));

        refreshTokenRepository.save(new RefreshToken(member, tokenHash, expiresAt));
    }
}
