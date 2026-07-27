package com.exhibitorreg.crew.exhibitorpass;

import static org.assertj.core.api.Assertions.assertThat;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.config.JpaAuditingConfig;
import com.exhibitorreg.organiser.ExhibitorRegistrationLink;
import com.exhibitorreg.organiser.ExhibitorRegistrationLinkRepository;
import com.exhibitorreg.publicregistration.Company;
import com.exhibitorreg.publicregistration.CompanyRepository;
import com.exhibitorreg.publicregistration.ExhibitorPerson;
import com.exhibitorreg.publicregistration.ExhibitorPersonRepository;
import com.exhibitorreg.publicregistration.ExhibitorSubmission;
import com.exhibitorreg.publicregistration.ExhibitorSubmissionRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/** Exercises the real Specification built by {@link ExhibitorPassService#list} against a real
 * database — the `q` search's implicit joins (e.g. company name) can't be verified against a
 * mocked repository, since a mock never actually evaluates the Criteria API predicate. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ExhibitorPassServiceSearchIntegrationTest {

    @Autowired
    private com.exhibitorreg.admin.EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExhibitorRegistrationLinkRepository linkRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ExhibitorSubmissionRepository submissionRepository;

    @Autowired
    private ExhibitorPersonRepository exhibitorPersonRepository;

    @Test
    void searchMatchesByCompanyNameAsWellAsPersonFields() {
        Event event = new Event();
        event.setName("Test Expo 2026");
        event.setStartDate(LocalDate.of(2026, 8, 1));
        event.setEndDate(LocalDate.of(2026, 8, 3));
        event.setActive(true);
        eventRepository.saveAndFlush(event);

        User organiser = new User();
        organiser.setUsername("organiser1");
        organiser.setPasswordHash("hashed-password");
        organiser.setRole(UserRole.ORGANISER);
        userRepository.saveAndFlush(organiser);

        ExhibitorRegistrationLink link = new ExhibitorRegistrationLink();
        link.setEvent(event);
        link.setCreatedBy(organiser);
        linkRepository.saveAndFlush(link);

        Company company = new Company();
        company.setName("Aspire Events And Exhibitions");
        companyRepository.saveAndFlush(company);

        ExhibitorSubmission submission = new ExhibitorSubmission();
        submission.setCompany(company);
        submission.setLink(link);
        submission.setDeclaredCount(1);
        submissionRepository.saveAndFlush(submission);

        ExhibitorPerson person = new ExhibitorPerson();
        person.setSubmission(submission);
        person.setCompany(company);
        person.setName("Alice");
        person.setDesignation("Sales Manager");
        exhibitorPersonRepository.saveAndFlush(person);

        ExhibitorPassService service = new ExhibitorPassService(exhibitorPersonRepository, userRepository);

        assertThat(service.list(null, null, null, "aspire")).extracting(dto -> dto.id()).containsExactly(person.getId());
        assertThat(service.list(null, null, null, "Alice")).extracting(dto -> dto.id()).containsExactly(person.getId());
        assertThat(service.list(null, null, null, "no-such-match")).isEmpty();
    }
}
