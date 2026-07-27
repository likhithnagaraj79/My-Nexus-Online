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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack: Admin creates+activates an Event → Organiser generates a Conference Delegate
 * link → public submission (captcha disabled in the test profile) creates one
 * {@code ConferenceDelegate} row directly → Crew lists and prints the badge. No check-in/QR
 * leg — Conference Delegates don't have Validator scanning (confirmed out of scope).
 *
 * <p>Mirrors {@link PublicRegistrationToCheckInFlowIntegrationTest}'s shape, own dedicated
 * named H2 instance for the same reasoning as that test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(InMemoryRedisTestConfig.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:exhibitor_registration_delegateflow_test;MODE=PostgreSQL")
class DelegateRegistrationAndPrintFlowIntegrationTest {

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
    void delegateRegistrationThroughPrintFlow() throws Exception {
        User admin = userRepository.saveAndFlush(seedUser("admin1", UserRole.ADMIN));
        User organiser = userRepository.saveAndFlush(seedUser("organiser1", UserRole.ORGANISER));
        User crew = userRepository.saveAndFlush(seedUser("crew1", UserRole.CREW));

        String adminToken = tokenFor(admin);
        String organiserToken = tokenFor(organiser);
        String crewToken = tokenFor(crew);

        // Admin: create + activate event.
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

        // Organiser: generate a Conference Delegate link.
        String linkResponse = mockMvc.perform(post("/api/organiser/delegate-links")
                        .header("Authorization", "Bearer " + organiserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String linkId = JsonPath.read(linkResponse, "$.id");

        mockMvc.perform(get("/api/public/delegate-links/" + linkId))
                .andExpect(status().isOk());

        // Public: submit the delegate registration (captcha disabled in test config).
        String submissionResponse = mockMvc.perform(post("/api/public/delegate-links/" + linkId + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"companyName\":\"Acme Exhibits\","
                                + "\"designation\":\"Sales\",\"mobileNumber\":\"9876543210\","
                                + "\"email\":\"alice@example.com\",\"recaptchaToken\":\"unused-because-disabled\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String delegateId = JsonPath.read(submissionResponse, "$.delegateId");

        // Crew: find the delegate, print.
        String listResponse = mockMvc.perform(get("/api/crew/conference-delegates?q=Alice")
                        .header("Authorization", "Bearer " + crewToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(listResponse, "$[0].id")).isEqualTo(delegateId);
        assertThat((String) JsonPath.read(listResponse, "$[0].companyName")).isEqualTo("Acme Exhibits");
        assertThat((Boolean) JsonPath.read(listResponse, "$[0].printed")).isFalse();

        mockMvc.perform(post("/api/crew/conference-delegates/print")
                        .header("Authorization", "Bearer " + crewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personIds\":[\"" + delegateId + "\"]}"))
                .andExpect(status().isOk());

        String afterPrintResponse = mockMvc.perform(get("/api/crew/conference-delegates?printed=true")
                        .header("Authorization", "Bearer " + crewToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(afterPrintResponse, "$[0].id")).isEqualTo(delegateId);
    }
}
