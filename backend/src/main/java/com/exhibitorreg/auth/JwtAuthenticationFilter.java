package com.exhibitorreg.auth;

import com.exhibitorreg.common.web.ProblemDetailResponseWriter;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves the Bearer access token into an {@link AuthenticatedPrincipal}. Invalid/missing
 * tokens are simply left unauthenticated (the filter chain's authorization rules then reject
 * protected endpoints via the entry point) — the one case this filter short-circuits itself
 * is the mustChangePassword gate, which is a business rule, not an authentication failure.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final Set<String> PATHS_ALLOWED_DURING_PASSWORD_CHANGE =
            Set.of("/api/auth/change-password", "/api/auth/logout");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                AuthenticatedPrincipal principal = jwtService.parseToken(token);

                if (principal.mustChangePassword()
                        && !PATHS_ALLOWED_DURING_PASSWORD_CHANGE.contains(request.getRequestURI())) {
                    ProblemDetailResponseWriter.write(
                            response,
                            objectMapper,
                            HttpStatus.FORBIDDEN,
                            "PASSWORD_CHANGE_REQUIRED",
                            "You must change your password before accessing this resource.");
                    return;
                }

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                var authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                // Leave unauthenticated; downstream authorization rules reject protected endpoints.
            }
        }

        filterChain.doFilter(request, response);
    }
}
