package com.exhibitorreg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.JwtService;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack: crew labour-pass creation validation (the {@code @AssertTrue} stall-number rule
 * through the real controller, not just the DTO-level unit test), role restriction, the
 * "no active event" conflict, event scoping across an event switch, and Organiser/Admin
 * visibility of {@code GET /api/organiser/labour-passes} (which — confirmed by reading
 * {@link com.exhibitorreg.organiser.OrganiserDashboardService} — is NOT event-scoped, unlike the
 * crew-facing endpoint).
 *
 * <p>Actor logins are covered by {@link AuthenticationFlowIntegrationTest}; here actors are
 * seeded directly and JWTs minted directly via {@link JwtService}, same pattern as
 * {@link PublicRegistrationToCheckInFlowIntegrationTest}. Uses its own dedicated named H2
 * instance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(InMemoryRedisTestConfig.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:exhibitor_registration_labourpass_test;MODE=PostgreSQL")
class LabourPassIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private static User seedUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("unused-in-this-test");
        user.setRole(role);
        user.setMustChangePassword(false);
        return user;
    }

    private String tokenFor(User user) {
        return jwtService.issueAccessToken(
                new AuthenticatedPrincipal(user.getId(), user.getUsername(), user.getRole(), false));
    }

    @Test
    void stallNumberRequiredForExhibitorAndFabricatorLabourPasses() throws Exception {
        User crew = userRepository.saveAndFlush(seedUser("crew-stall", UserRole.CREW));
        String crewToken = tokenFor(crew);

        // No active event yet — VENDOR (no stallNumber needed) still hits the event check first.
        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":2,\"phoneNumber\":\"9000000001\"}"))
                .andExpect(status().isConflict());

        createAndActivateEvent("crew-admin-1");

        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"EXHIBITOR\",\"passCount\":1,\"phoneNumber\":\"9000000002\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"FABRICATOR_LABOUR\",\"passCount\":1,\"phoneNumber\":\"9000000003\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":3,\"phoneNumber\":\"9000000004\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"passType\":\"EXHIBITOR\",\"passCount\":1,\"phoneNumber\":\"9000000005\",\"stallNumber\":\"A-12\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void nonCrewRolesCannotCreateLabourPasses() throws Exception {
        User organiser = userRepository.saveAndFlush(seedUser("organiser-role-check", UserRole.ORGANISER));
        String organiserToken = tokenFor(organiser);

        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + organiserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":1,\"phoneNumber\":\"9000000006\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void labourPassesAreScopedToTheActiveEventAtCreationTime() throws Exception {
        User crew = userRepository.saveAndFlush(seedUser("crew-scoping", UserRole.CREW));
        String crewToken = tokenFor(crew);

        String eventAId = createAndActivateEvent("admin-scoping-a");
        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":1,\"phoneNumber\":\"9000000007\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":1,\"phoneNumber\":\"9000000008\"}"))
                .andExpect(status().isCreated());

        // Activating a second event auto-deactivates the first (AdminEventService.activate).
        String eventBId = createAndActivateEvent("admin-scoping-b");
        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":1,\"phoneNumber\":\"9000000009\"}"))
                .andExpect(status().isCreated());

        String eventAPasses = mockMvc.perform(get("/api/crew/labour-passes?eventId=" + eventAId)
                        .header("Authorization", "Bearer " + crewToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(((JSONArray) JsonPath.read(eventAPasses, "$")).size()).isEqualTo(2);

        String eventBPasses = mockMvc.perform(get("/api/crew/labour-passes?eventId=" + eventBId)
                        .header("Authorization", "Bearer " + crewToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(((JSONArray) JsonPath.read(eventBPasses, "$")).size()).isEqualTo(1);
    }

    @Test
    void onlyOrganiserAndAdminCanListAllLabourPasses() throws Exception {
        User crew = userRepository.saveAndFlush(seedUser("crew-visibility", UserRole.CREW));
        User organiser = userRepository.saveAndFlush(seedUser("organiser-visibility", UserRole.ORGANISER));
        User admin = userRepository.saveAndFlush(seedUser("admin-visibility", UserRole.ADMIN));
        User validator = userRepository.saveAndFlush(seedUser("validator-visibility", UserRole.VALIDATOR));
        String crewToken = tokenFor(crew);

        createAndActivateEvent("admin-visibility-event");
        mockMvc.perform(post("/api/crew/labour-passes")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passType\":\"VENDOR\",\"passCount\":1,\"phoneNumber\":\"9000000010\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/organiser/labour-passes").header("Authorization", "Bearer " + tokenFor(organiser)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/organiser/labour-passes").header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/organiser/labour-passes").header("Authorization", "Bearer " + crewToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/organiser/labour-passes").header("Authorization", "Bearer " + tokenFor(validator)))
                .andExpect(status().isForbidden());
    }

    /** Creates + activates a uniquely-named event so each test/step gets a fresh active event. */
    private String createAndActivateEvent(String uniqueAdminUsername) throws Exception {
        User admin = userRepository
                .findByUsername(uniqueAdminUsername)
                .orElseGet(() -> userRepository.saveAndFlush(seedUser(uniqueAdminUsername, UserRole.ADMIN)));
        String adminToken = tokenFor(admin);

        String eventResponse = mockMvc.perform(post("/api/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + uniqueAdminUsername
                                + "-expo\",\"startDate\":\"2026-08-01\",\"endDate\":\"2026-08-03\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String eventId = JsonPath.read(eventResponse, "$.id");

        mockMvc.perform(
                        post("/api/admin/events/" + eventId + "/activate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        return eventId;
    }
}
