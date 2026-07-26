package com.exhibitorreg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.JwtService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack: Admin creates+activates an Event and EventDay → Organiser generates a public
 * link → public submission (captcha disabled in the test profile) creates Company/Submission/
 * People → Crew prints+issues a badge → Validator scans the QR payload (= ExhibitorPerson.id)
 * → Organiser's CSV export contains the check-in row.
 *
 * <p>Actor logins themselves are already covered end-to-end by
 * {@link AuthenticationFlowIntegrationTest}; here, actors are seeded directly and JWTs minted
 * directly via {@link JwtService} so this test stays focused on the registration/badge/scan
 * business flow. Uses its own dedicated named H2 instance, same reasoning as that test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(InMemoryRedisTestConfig.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:exhibitor_registration_regflow_test;MODE=PostgreSQL")
class PublicRegistrationToCheckInFlowIntegrationTest {

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
    void publicRegistrationThroughCheckInFlow() throws Exception {
        User admin = userRepository.saveAndFlush(seedUser("admin1", UserRole.ADMIN));
        User organiser = userRepository.saveAndFlush(seedUser("organiser1", UserRole.ORGANISER));
        User crew = userRepository.saveAndFlush(seedUser("crew1", UserRole.CREW));
        User validator = userRepository.saveAndFlush(seedUser("validator1", UserRole.VALIDATOR));

        String adminToken = tokenFor(admin);
        String organiserToken = tokenFor(organiser);
        String crewToken = tokenFor(crew);
        String validatorToken = tokenFor(validator);

        // Admin: create + activate event — Day 1/2/3 are auto-created with the event.
        String eventResponse = mockMvc.perform(post("/api/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Expo 2026\",\"startDate\":\"2026-08-01\",\"endDate\":\"2026-08-03\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String eventId = JsonPath.read(eventResponse, "$.id");

        mockMvc.perform(post("/api/admin/events/" + eventId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String eventDaysResponse = mockMvc.perform(get("/api/admin/events/" + eventId + "/days")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String eventDayId = JsonPath.read(eventDaysResponse, "$[0].id");

        // Organiser: generate public link
        String linkResponse = mockMvc.perform(post("/api/organiser/links")
                        .header("Authorization", "Bearer " + organiserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String linkId = JsonPath.read(linkResponse, "$.id");

        mockMvc.perform(get("/api/public/links/" + linkId))
                .andExpect(status().isOk());

        // Public: submit registration (captcha disabled in test config, so any token is accepted)
        String submissionResponse = mockMvc.perform(post("/api/public/links/" + linkId + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme Exhibits\","
                                + "\"people\":[{\"name\":\"Alice\",\"designation\":\"Sales\"}],"
                                + "\"recaptchaToken\":\"unused-because-disabled\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String companyId = JsonPath.read(submissionResponse, "$.companyId");

        // Crew: find the person, print by company, then issue
        String listResponse = mockMvc.perform(get("/api/crew/exhibitor-passes?companyId=" + companyId)
                        .header("Authorization", "Bearer " + crewToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String personId = JsonPath.read(listResponse, "$[0].id");
        assertThat((String) JsonPath.read(listResponse, "$[0].name")).isEqualTo("Alice");

        mockMvc.perform(post("/api/crew/exhibitor-passes/print")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":\"" + companyId + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/crew/exhibitor-passes/" + personId + "/issue")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"9876543210\"}"))
                .andExpect(status().isOk());

        // Validator: scan the badge's QR payload (= ExhibitorPerson.id) for the event day
        String scanResponse = mockMvc.perform(post("/api/validator/scans")
                        .header("Authorization", "Bearer " + validatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventDayId\":\"" + eventDayId + "\",\"qrPayload\":\"" + personId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((Boolean) JsonPath.read(scanResponse, "$.alreadyCheckedInToday")).isFalse();
        assertThat((String) JsonPath.read(scanResponse, "$.personName")).isEqualTo("Alice");

        // Organiser: CSV export contains the check-in row
        String csv = mockMvc.perform(get("/api/organiser/check-ins/export?eventDayId=" + eventDayId)
                        .header("Authorization", "Bearer " + organiserToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("Alice").contains("Acme Exhibits").contains("validator1");
    }
}
