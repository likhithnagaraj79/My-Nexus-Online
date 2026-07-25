package com.exhibitorreg.publicregistration;

import static org.assertj.core.api.Assertions.assertThat;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.admin.EventDay;
import com.exhibitorreg.admin.EventDayRepository;
import com.exhibitorreg.admin.EventRepository;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.config.JpaAuditingConfig;
import com.exhibitorreg.organiser.ExhibitorRegistrationLink;
import com.exhibitorreg.organiser.ExhibitorRegistrationLinkRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ExhibitorPersonRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventDayRepository eventDayRepository;

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
    void savesAndFetchesExhibitorPeopleByCompany() {
        Event event = new Event();
        event.setName("Test Expo 2026");
        event.setStartDate(LocalDate.of(2026, 8, 1));
        event.setEndDate(LocalDate.of(2026, 8, 3));
        event.setActive(true);
        eventRepository.saveAndFlush(event);

        EventDay eventDay = new EventDay();
        eventDay.setEvent(event);
        eventDay.setDayNumber(1);
        eventDay.setDate(event.getStartDate());
        eventDayRepository.saveAndFlush(eventDay);

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
        company.setName("Acme Exhibits Pvt Ltd");
        companyRepository.saveAndFlush(company);

        ExhibitorSubmission submission = new ExhibitorSubmission();
        submission.setCompany(company);
        submission.setLink(link);
        submission.setDeclaredCount(2);
        submissionRepository.saveAndFlush(submission);

        ExhibitorPerson person1 = new ExhibitorPerson();
        person1.setSubmission(submission);
        person1.setCompany(company);
        person1.setName("Alice");
        person1.setDesignation("Sales Manager");
        exhibitorPersonRepository.saveAndFlush(person1);

        ExhibitorPerson person2 = new ExhibitorPerson();
        person2.setSubmission(submission);
        person2.setCompany(company);
        person2.setName("Bob");
        person2.setDesignation("Booth Lead");
        exhibitorPersonRepository.saveAndFlush(person2);

        assertThat(person1.getId()).isNotNull();
        assertThat(person2.getId()).isNotNull();

        List<ExhibitorPerson> people = exhibitorPersonRepository.findByCompanyId(company.getId());
        assertThat(people).hasSize(2).extracting(ExhibitorPerson::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }
}
