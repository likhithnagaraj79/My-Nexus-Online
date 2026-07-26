package com.exhibitorreg.common;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exhibitorreg.admin.AdminEventService;
import com.exhibitorreg.admin.dto.EventResponse;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.JwtService;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.config.SecurityConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommonEventController.class)
@Import(SecurityConfig.class)
class CommonEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminEventService adminEventService;

    @MockitoBean
    private JwtService jwtService;

    private static Authentication authFor(UserRole role) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(UUID.randomUUID(), "user1", role, false);
        return new TestingAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/common/events/active")).andExpect(status().isUnauthorized());
    }

    @Test
    void crewCanReachActiveEvent() throws Exception {
        when(adminEventService.getActiveEvent()).thenReturn(
                new EventResponse(UUID.randomUUID(), "Expo", LocalDate.now(), LocalDate.now().plusDays(2), true));

        mockMvc.perform(get("/api/common/events/active").with(authentication(authFor(UserRole.CREW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Expo"));
    }

    @Test
    void organiserCanReachActiveEvent() throws Exception {
        when(adminEventService.getActiveEvent()).thenReturn(
                new EventResponse(UUID.randomUUID(), "Expo", LocalDate.now(), LocalDate.now().plusDays(2), true));

        mockMvc.perform(get("/api/common/events/active").with(authentication(authFor(UserRole.ORGANISER))))
                .andExpect(status().isOk());
    }

    @Test
    void validatorCanReachEventDays() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(adminEventService.listEventDays(eventId)).thenReturn(List.of());

        mockMvc.perform(get("/api/common/events/" + eventId + "/days")
                        .with(authentication(authFor(UserRole.VALIDATOR))))
                .andExpect(status().isOk());
    }
}
