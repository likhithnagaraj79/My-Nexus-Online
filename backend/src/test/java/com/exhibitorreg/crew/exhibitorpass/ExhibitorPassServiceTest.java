package com.exhibitorreg.crew.exhibitorpass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.crew.exhibitorpass.dto.IssueRequest;
import com.exhibitorreg.crew.exhibitorpass.dto.PrintRequest;
import com.exhibitorreg.publicregistration.Company;
import com.exhibitorreg.publicregistration.ExhibitorPerson;
import com.exhibitorreg.publicregistration.ExhibitorPersonRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExhibitorPassServiceTest {

    @Mock
    private ExhibitorPersonRepository exhibitorPersonRepository;

    @Mock
    private UserRepository userRepository;

    private ExhibitorPassService service;

    @BeforeEach
    void setUp() {
        service = new ExhibitorPassService(exhibitorPersonRepository, userRepository);
    }

    private static Company companyWithId() {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        company.setName("Acme Exhibits");
        return company;
    }

    private static ExhibitorPerson personFor(Company company) {
        ExhibitorPerson person = new ExhibitorPerson();
        ReflectionTestUtils.setField(person, "id", UUID.randomUUID());
        person.setCompany(company);
        person.setName("Alice");
        person.setDesignation("Sales");
        return person;
    }

    private static User crewWithId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("crew1");
        user.setRole(UserRole.CREW);
        return user;
    }

    @Test
    void printRejectsBothPersonIdsAndCompanyId() {
        Company company = companyWithId();
        User crew = crewWithId();
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);

        assertThatThrownBy(() -> service.print(
                        principal, new PrintRequest(List.of(UUID.randomUUID()), company.getId())))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void printRejectsNeitherPersonIdsNorCompanyId() {
        User crew = crewWithId();
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);

        assertThatThrownBy(() -> service.print(principal, new PrintRequest(null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void printByCompanyIdMarksAllPeopleInThatCompanyAsPrinted() {
        Company company = companyWithId();
        ExhibitorPerson person1 = personFor(company);
        ExhibitorPerson person2 = personFor(company);
        User crew = crewWithId();
        when(exhibitorPersonRepository.findByCompanyId(company.getId())).thenReturn(List.of(person1, person2));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);
        var results = service.print(principal, new PrintRequest(null, company.getId()));

        assertThat(results).hasSize(2);
        assertThat(person1.isPrinted()).isTrue();
        assertThat(person1.getPrintedBy()).isEqualTo(crew);
        assertThat(person2.isPrinted()).isTrue();
    }

    @Test
    void printByPersonIdsIsIdempotentForAlreadyPrintedPeople() {
        Company company = companyWithId();
        ExhibitorPerson person = personFor(company);
        person.setPrinted(true);
        User crew = crewWithId();
        when(exhibitorPersonRepository.findAllById(List.of(person.getId()))).thenReturn(List.of(person));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);
        var results = service.print(principal, new PrintRequest(List.of(person.getId()), null));

        assertThat(results).hasSize(1);
        assertThat(person.isPrinted()).isTrue();
    }

    @Test
    void issueDoesNotRequirePriorPrinting() {
        Company company = companyWithId();
        ExhibitorPerson person = personFor(company);
        person.setPrinted(false);
        User crew = crewWithId();
        when(exhibitorPersonRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);
        var result = service.issue(principal, person.getId(), new IssueRequest("9876543210"));

        assertThat(result.issued()).isTrue();
        assertThat(person.getIssuedPhoneNumber()).isEqualTo("9876543210");
        assertThat(person.getIssuedBy()).isEqualTo(crew);
    }

    @Test
    void generateQrPngUsesPersonIdAsPayload() {
        Company company = companyWithId();
        ExhibitorPerson person = personFor(company);
        when(exhibitorPersonRepository.findById(person.getId())).thenReturn(Optional.of(person));

        byte[] png = service.generateQrPng(person.getId());

        assertThat(png).isNotEmpty();
        // PNG file signature
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }
}
