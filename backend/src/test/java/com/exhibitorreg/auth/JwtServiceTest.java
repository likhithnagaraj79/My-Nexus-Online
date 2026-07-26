package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-jwt-signing-secret-long-enough-for-hs256-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 30);

    @Test
    void issuesAndParsesRoundTrip() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), "admin", UserRole.ADMIN, false);

        String token = jwtService.issueAccessToken(principal);
        AuthenticatedPrincipal parsed = jwtService.parseToken(token);

        assertThat(parsed).isEqualTo(principal);
    }

    @Test
    void carriesMustChangePasswordClaim() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), "crew1", UserRole.CREW, true);

        String token = jwtService.issueAccessToken(principal);
        AuthenticatedPrincipal parsed = jwtService.parseToken(token);

        assertThat(parsed.mustChangePassword()).isTrue();
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-signing-secret-0123456789", 30);
        String token = otherService.issueAccessToken(
                new AuthenticatedPrincipal(UUID.randomUUID(), "admin", UserRole.ADMIN, false));

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseToken("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
