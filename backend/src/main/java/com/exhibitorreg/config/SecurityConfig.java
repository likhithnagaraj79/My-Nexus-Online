package com.exhibitorreg.config;

import com.exhibitorreg.auth.JwtAuthenticationFilter;
import com.exhibitorreg.auth.JwtService;
import com.exhibitorreg.common.web.ProblemDetailResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api/auth/login",
        "/api/auth/login/totp",
        "/api/auth/refresh",
        "/api/public/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/actuator/health"
    };

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // Read-only event/day discovery shared by every authenticated role
                        // (Crew/Organiser/Validator otherwise have no way to look up an eventId).
                        .requestMatchers("/api/common/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Shared with Admin (spec: "visible to Organiser and Admin too") — role choice
                        // is enforced by @PreAuthorize on the controller method, not the URL rule below.
                        .requestMatchers("/api/organiser/labour-passes").authenticated()
                        .requestMatchers("/api/organiser/**").hasRole("ORGANISER")
                        // Shared with Organiser/Admin ("all exhibitor submitted details visible to
                        // Organiser") — only the read-only list, not print/issue — role choice is
                        // enforced by @PreAuthorize on the controller method, not this URL rule.
                        .requestMatchers(HttpMethod.GET, "/api/crew/exhibitor-passes").authenticated()
                        .requestMatchers("/api/crew/**").hasRole("CREW")
                        .requestMatchers("/api/validator/**").hasRole("VALIDATOR")
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, authException) ->
                                ProblemDetailResponseWriter.write(
                                        response,
                                        objectMapper,
                                        HttpStatus.UNAUTHORIZED,
                                        "UNAUTHENTICATED",
                                        "Authentication is required to access this resource."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                ProblemDetailResponseWriter.write(
                                        response,
                                        objectMapper,
                                        HttpStatus.FORBIDDEN,
                                        "FORBIDDEN",
                                        "You do not have permission to access this resource.")))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
