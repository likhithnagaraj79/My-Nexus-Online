package com.exhibitorreg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack: Admin creates a Crew account → deactivate → login blocked → reactivate → login
 * succeeds again → regenerate TOTP secret → the old code is rejected, the new one works.
 *
 * <p>Deliberately does NOT assert audit-log entries for deactivate/reactivate/regenerate:
 * confirmed by reading {@code AdminActorService} that only {@code unlock()} writes an
 * {@link com.exhibitorreg.common.AuditLog} row today — {@code setActive()} and
 * {@code regenerateTotpQr()} don't. That gap (already exercised for {@code unlock()} by
 * {@link AuthenticationFlowIntegrationTest}) is out of scope for a testing-only phase; this
 * test instead asserts the actual observable behavior of each transition.
 *
 * <p>Uses its own dedicated named H2 instance, same pattern as the other integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(InMemoryRedisTestConfig.class)
@TestPropertySource(
        properties = "spring.datasource.url=jdbc:h2:mem:exhibitor_registration_actorlifecycle_test;MODE=PostgreSQL")
class ActorLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deactivateReactivateAndRegenerateTotp() throws Exception {
        User admin = new User();
        admin.setUsername("lifecycle-admin");
        admin.setPasswordHash(passwordEncoder.encode("AdminPass123"));
        admin.setRole(UserRole.ADMIN);
        admin.setMustChangePassword(false);
        userRepository.saveAndFlush(admin);

        String adminLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"role\":\"ADMIN\",\"username\":\"lifecycle-admin\",\"password\":\"AdminPass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = JsonPath.read(adminLoginResponse, "$.tokens.accessToken");

        // Create Crew.
        String createCrewResponse = mockMvc.perform(post("/api/admin/actors/crew")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lifecycle-crew\",\"temporaryPassword\":\"CrewPass123\","
                                + "\"aadharNumber\":\"123456789099\",\"phoneNumber\":\"9876500001\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String crewId = JsonPath.read(createCrewResponse, "$.userId");

        User crewAfterCreate = userRepository.findByUsername("lifecycle-crew").orElseThrow();
        String originalTotpSecret = crewAfterCreate.getTotpSecret();
        assertThat(originalTotpSecret).isNotBlank();

        // Deactivate — login now behaves exactly like an unknown user (401, not 403/404).
        mockMvc.perform(patch("/api/admin/actors/" + crewId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"role\":\"CREW\",\"username\":\"lifecycle-crew\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isUnauthorized());

        // Reactivate — login works again (TOTP still required, unchanged secret).
        mockMvc.perform(patch("/api/admin/actors/" + crewId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"role\":\"CREW\",\"username\":\"lifecycle-crew\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpRequired").value(true));

        // Regenerate TOTP — the service returns a QR for the new secret; read the new secret
        // straight from the DB rather than decoding the QR image.
        mockMvc.perform(post("/api/admin/actors/" + crewId + "/totp-qr/regenerate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpQrPngBase64").isNotEmpty());

        User crewAfterRegenerate = userRepository.findByUsername("lifecycle-crew").orElseThrow();
        String newTotpSecret = crewAfterRegenerate.getTotpSecret();
        assertThat(newTotpSecret).isNotBlank().isNotEqualTo(originalTotpSecret);

        // Old code, on a fresh login ticket, is rejected.
        String loginResponseForOldCode = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"role\":\"CREW\",\"username\":\"lifecycle-crew\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String loginTicketForOldCode = JsonPath.read(loginResponseForOldCode, "$.loginTicketId");
        String oldCode = new DefaultCodeGenerator()
                .generate(originalTotpSecret, System.currentTimeMillis() / 1000L / 30L);

        mockMvc.perform(post("/api/auth/login/totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginTicketId\":\"" + loginTicketForOldCode + "\",\"code\":\"" + oldCode
                                + "\"}"))
                .andExpect(status().isUnauthorized());

        // New code, on a fresh login ticket (the failed attempt above consumed the previous one),
        // succeeds.
        String loginResponseForNewCode = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"role\":\"CREW\",\"username\":\"lifecycle-crew\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String loginTicketForNewCode = JsonPath.read(loginResponseForNewCode, "$.loginTicketId");
        String newCode = new DefaultCodeGenerator()
                .generate(newTotpSecret, System.currentTimeMillis() / 1000L / 30L);

        mockMvc.perform(post("/api/auth/login/totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginTicketId\":\"" + loginTicketForNewCode + "\",\"code\":\"" + newCode
                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").exists());
    }
}
