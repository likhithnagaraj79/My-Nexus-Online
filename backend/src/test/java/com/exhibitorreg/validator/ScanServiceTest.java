package com.exhibitorreg.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.exhibitorreg.admin.EventDay;
import com.exhibitorreg.admin.EventDayRepository;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.publicregistration.Company;
import com.exhibitorreg.publicregistration.ExhibitorPerson;
import com.exhibitorreg.publicregistration.ExhibitorPersonRepository;
import com.exhibitorreg.validator.dto.ScanRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    @Mock
    private CheckInScanRepository checkInScanRepository;

    @Mock
    private ExhibitorPersonRepository exhibitorPersonRepository;

    @Mock
    private EventDayRepository eventDayRepository;

    @Mock
    private UserRepository userRepository;

    private ScanService service;

    @BeforeEach
    void setUp() {
        service = new ScanService(checkInScanRepository, exhibitorPersonRepository, eventDayRepository, userRepository);
    }

    private static ExhibitorPerson personWithId() {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        company.setName("Acme");

        ExhibitorPerson person = new ExhibitorPerson();
        ReflectionTestUtils.setField(person, "id", UUID.randomUUID());
        person.setCompany(company);
        person.setName("Alice");
        return person;
    }

    private static EventDay eventDayWithId() {
        EventDay eventDay = new EventDay();
        ReflectionTestUtils.setField(eventDay, "id", UUID.randomUUID());
        return eventDay;
    }

    private static User validatorWithId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("validator1");
        user.setRole(UserRole.VALIDATOR);
        return user;
    }

    @Test
    void scanWithNonUuidPayloadThrowsIllegalArgument() {
        User validator = validatorWithId();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(validator.getId(), validator.getUsername(), UserRole.VALIDATOR, false);

        assertThatThrownBy(() -> service.scan(principal, new ScanRequest(UUID.randomUUID(), "not-a-uuid")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scanWithUnknownPersonIdThrowsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(exhibitorPersonRepository.findById(unknownId)).thenReturn(Optional.empty());
        User validator = validatorWithId();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(validator.getId(), validator.getUsername(), UserRole.VALIDATOR, false);

        assertThatThrownBy(() -> service.scan(principal, new ScanRequest(UUID.randomUUID(), unknownId.toString())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void firstScanOfTheDayIsNotFlaggedAsDuplicate() {
        ExhibitorPerson person = personWithId();
        EventDay eventDay = eventDayWithId();
        User validator = validatorWithId();
        when(exhibitorPersonRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(eventDayRepository.findById(eventDay.getId())).thenReturn(Optional.of(eventDay));
        when(userRepository.findById(validator.getId())).thenReturn(Optional.of(validator));
        when(checkInScanRepository.existsByExhibitorPersonIdAndEventDayId(person.getId(), eventDay.getId()))
                .thenReturn(false);

        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(validator.getId(), validator.getUsername(), UserRole.VALIDATOR, false);
        var response = service.scan(principal, new ScanRequest(eventDay.getId(), person.getId().toString()));

        assertThat(response.alreadyCheckedInToday()).isFalse();
    }

    @Test
    void repeatScanIsRecordedButFlaggedAsDuplicateNotBlocked() {
        ExhibitorPerson person = personWithId();
        EventDay eventDay = eventDayWithId();
        User validator = validatorWithId();
        when(exhibitorPersonRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(eventDayRepository.findById(eventDay.getId())).thenReturn(Optional.of(eventDay));
        when(userRepository.findById(validator.getId())).thenReturn(Optional.of(validator));
        when(checkInScanRepository.existsByExhibitorPersonIdAndEventDayId(person.getId(), eventDay.getId()))
                .thenReturn(true);

        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(validator.getId(), validator.getUsername(), UserRole.VALIDATOR, false);
        var response = service.scan(principal, new ScanRequest(eventDay.getId(), person.getId().toString()));

        assertThat(response.alreadyCheckedInToday()).isTrue();
        assertThat(response.personName()).isEqualTo("Alice");
    }
}
