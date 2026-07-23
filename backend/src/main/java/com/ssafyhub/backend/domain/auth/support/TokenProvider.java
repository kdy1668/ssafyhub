package com.ssafyhub.backend.domain.auth.support;

import com.ssafyhub.backend.global.error.BusinessException;
import com.ssafyhub.backend.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMillis;

    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-millis}") long accessTokenExpirationMillis
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
    }

    public String createAccessToken(Long memberId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(JwtConstants.ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Long getMemberId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getRole(Claims claims) {
        return claims.get(JwtConstants.ROLE_CLAIM, String.class);
    }
}
