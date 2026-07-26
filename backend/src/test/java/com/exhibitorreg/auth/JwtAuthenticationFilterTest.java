package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "filter-test-jwt-signing-secret-long-enough-for-hs256-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 30);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, new ObjectMapper());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationForValidToken() throws Exception {
        String token = jwtService.issueAccessToken(
                new AuthenticatedPrincipal(UUID.randomUUID(), "admin1", UserRole.ADMIN, false));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/actors");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedPrincipal.class);
        assertThat(((AuthenticatedPrincipal) authentication.getPrincipal()).username()).isEqualTo("admin1");
        verify(chain).doFilter(request, response);
    }

    @Test
    void leavesUnauthenticatedForMissingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/actors");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void leavesUnauthenticatedForMalformedToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/actors");
        request.addHeader("Authorization", "Bearer not-a-real-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void shortCircuitsWithForbiddenWhenPasswordChangeRequired() throws Exception {
        String token = jwtService.issueAccessToken(
                new AuthenticatedPrincipal(UUID.randomUUID(), "crew1", UserRole.CREW, true));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/crew/labour-passes");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PASSWORD_CHANGE_REQUIRED");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsChangePasswordEndpointDespiteMustChangePasswordFlag() throws Exception {
        String token = jwtService.issueAccessToken(
                new AuthenticatedPrincipal(UUID.randomUUID(), "crew1", UserRole.CREW, true));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/change-password");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(request, response);
    }
}
