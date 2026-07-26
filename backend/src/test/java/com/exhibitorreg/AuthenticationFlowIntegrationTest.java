package com.exhibitorreg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Full-stack: bootstrap Admin (seeded directly, since RootAdminBootstrapRunner itself already
 * has its own dedicated unit test) → forced password change → create Organiser/Crew →
 * Crew TOTP login → 5 failed logins locks the account → Admin unlock.
 *
 * <p>Unlike {@code @DataJpaTest}, {@code @SpringBootTest} does not roll back after each test,
 * so this class uses its own dedicated named H2 instance (distinct from the one shared by
 * every {@code @DataJpaTest}) to avoid leaking committed users (e.g. "admin1") into unrelated
 * tests running in the same JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(InMemoryRedisTestConfig.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:exhibitor_registration_authflow_test;MODE=PostgreSQL")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void fullAuthenticationLifecycle() throws Exception {
        User admin = new User();
        admin.setUsername("admin1");
        admin.setPasswordHash(passwordEncoder.encode("AdminPass123"));
        admin.setRole(UserRole.ADMIN);
        admin.setMustChangePassword(true);
        userRepository.saveAndFlush(admin);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"username\":\"admin1\",\"password\":\"AdminPass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminAccessToken = JsonPath.read(loginResponse, "$.tokens.accessToken");

        // Blocked from other endpoints until the forced password change is done.
        mockMvc.perform(get("/api/admin/actors")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isForbidden());

        String changePwResponse = mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"AdminPass123\",\"newPassword\":\"NewAdminPass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String freshAdminAccessToken = JsonPath.read(changePwResponse, "$.accessToken");

        mockMvc.perform(get("/api/admin/actors")
                        .header("Authorization", "Bearer " + freshAdminAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/actors/organisers")
                        .header("Authorization", "Bearer " + freshAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"username\":\"organiser1\",\"email\":\"organiser1@example.com\",\"temporaryPassword\":\"OrgPass123\"}"))
                .andExpect(status().isCreated());

        String createCrewResponse = mockMvc.perform(post("/api/admin/actors/crew")
                        .header("Authorization", "Bearer " + freshAdminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"username\":\"crew1\",\"temporaryPassword\":\"CrewPass123\",\"aadharNumber\":\"123456789012\",\"phoneNumber\":\"9876543210\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(createCrewResponse, "$.totpQrPngBase64")).isNotBlank();

        String crewLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CREW\",\"username\":\"crew1\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpRequired").value(true))
                .andReturn().getResponse().getContentAsString();
        String loginTicketId = JsonPath.read(crewLoginResponse, "$.loginTicketId");

        User crewUser = userRepository.findByUsername("crew1").orElseThrow();
        String code = new DefaultCodeGenerator()
                .generate(crewUser.getTotpSecret(), System.currentTimeMillis() / 1000L / 30L);

        mockMvc.perform(post("/api/auth/login/totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginTicketId\":\"" + loginTicketId + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").exists());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"CREW\",\"username\":\"crew1\",\"password\":\"WrongPassword\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CREW\",\"username\":\"crew1\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isLocked());

        mockMvc.perform(post("/api/admin/actors/" + crewUser.getId() + "/unlock")
                        .header("Authorization", "Bearer " + freshAdminAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CREW\",\"username\":\"crew1\",\"password\":\"CrewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpRequired").value(true));
    }
}
