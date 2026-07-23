package com.ssafyhub.backend.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.auth.security.CustomUserDetails;
import com.ssafyhub.backend.domain.auth.support.JwtConstants;
import com.ssafyhub.backend.domain.auth.support.TokenProvider;
import com.ssafyhub.backend.domain.member.entity.Member;
import com.ssafyhub.backend.domain.member.repository.MemberRepository;
import com.ssafyhub.backend.global.common.ApiResponse;
import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = extractAccessToken(request);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = tokenProvider.parseClaims(accessToken);
            Long memberId = tokenProvider.getMemberId(claims);

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

            CustomUserDetails userDetails = CustomUserDetails.from(member);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            writeErrorResponse(response, e.getErrorCode());
        }
    }

    private String extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (JwtConstants.ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()))
        );
    }
}
