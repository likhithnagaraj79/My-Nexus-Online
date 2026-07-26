package com.exhibitorreg.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exhibitorreg.auth.dto.ChangePasswordRequest;
import com.exhibitorreg.auth.dto.LoginResponse;
import com.exhibitorreg.auth.dto.TokenPair;
import com.exhibitorreg.config.SecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private static Authentication authFor(UUID userId, UserRole role) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, "user1", role, false);
        return new TestingAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    void loginDelegatesToServiceAndReturnsResponseBody() throws Exception {
        when(authService.login(any(), anyString(), any()))
                .thenReturn(LoginResponse.authenticated(new TokenPair("access", "refresh", false)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"username\":\"admin1\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").value("access"));
    }

    @Test
    void loginWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refreshDelegatesToService() throws Exception {
        when(authService.refresh(eq("old-refresh"))).thenReturn(new TokenPair("new-access", "new-refresh", false));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void logoutWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithAuthenticationCallsService() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/auth/logout")
                        .with(authentication(authFor(userId, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordWithAuthenticationReturnsTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.changePassword(any(), any(ChangePasswordRequest.class)))
                .thenReturn(new TokenPair("fresh-access", "fresh-refresh", false));

        mockMvc.perform(post("/api/auth/change-password")
                        .with(authentication(authFor(userId, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old12345\",\"newPassword\":\"newpassword1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fresh-access"));
    }
}
