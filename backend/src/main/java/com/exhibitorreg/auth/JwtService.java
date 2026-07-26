package com.exhibitorreg.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and parses short-lived JWT access tokens. Refresh tokens are separate opaque
 * strings stored in Redis (see {@link RefreshTokenService}) — never JWTs themselves,
 * so they carry no independently-decodable claims.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";

    private final SecretKey signingKey;
    private final long accessTokenTtlMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String issueAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.username())
                .claim(CLAIM_USER_ID, principal.userId().toString())
                .claim(CLAIM_ROLE, principal.role().name())
                .claim(CLAIM_MUST_CHANGE_PASSWORD, principal.mustChangePassword())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * @throws JwtException if the token is malformed, expired, or fails signature verification
     */
    public AuthenticatedPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedPrincipal(
                UUID.fromString(claims.get(CLAIM_USER_ID, String.class)),
                claims.getSubject(),
                UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                Boolean.TRUE.equals(claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class)));
    }
}
