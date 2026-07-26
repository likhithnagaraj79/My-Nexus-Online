package com.exhibitorreg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.jayway.jsonpath.JsonPath;
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
 * Full-stack: with {@code app.totp.enabled=false}, creating a Crew account returns no QR code
 * and logging in as Crew is single-step (identical to Organiser) — the {@code app.totp.enabled}
 * toggle added so Google Authenticator can be turned off without a code change (see
 * {@code application-prod.yml}: {@code APP_TOTP_ENABLED}).
 *
 * <p>Every other integration test in this codebase runs with the default ({@code dev} profile's)
 * {@code app.totp.enabled=true}, so this is the one place that exercises the disabled path.
 * Uses its own dedicated named H2 instance, same pattern as the other integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(InMemoryRedisTestConfig.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:exhibitor_registration_totpdisabled_test;MODE=PostgreSQL",
            "app.totp.enabled=false"
        })
class TotpDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void crewAccountsSkipTotpEntirelyWhenDisabled() throws Exception {
        User admin = new User();
        admin.setUsername("totp-disabled-admin");
        admin.setPasswordHash(passwordEncoder.encode("AdminPass123"));
        admin.setRole(UserRole.ADMIN);
        admin.setMustChangePassword(false);
        userRepository.saveAndFlush(admin);

        String adminLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"username\":\"totp-disabled-admin\",\"password\":\"AdminPass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = JsonPath.read(adminLoginResponse, "$.tokens.accessToken");

        mockMvc.perform(post("/api/admin/actors/crew")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"totp-disabled-crew\",\"temporaryPassword\":\"CrewPass123\","
                                + "\"aadharNumber\":\"123456789012\",\"phoneNumber\":\"9876500001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totpQrPngBase64").doesNotExist());
        assertThat(userRepository.findByUsername("totp-disabled-crew").orElseThrow().getTotpSecret()).isNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"role\":\"CREW\",\"username\":\"totp-disabled-crew\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpRequired").value(false))
                .andExpect(jsonPath("$.tokens.accessToken").exists());
    }
}
