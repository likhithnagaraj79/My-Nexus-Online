package com.exhibitorreg.crew.labourpass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.admin.EventRepository;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.crew.labourpass.dto.CreateLabourPassRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LabourPassServiceTest {

    @Mock
    private LabourPassRepository labourPassRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    private LabourPassService service;

    @BeforeEach
    void setUp() {
        service = new LabourPassService(labourPassRepository, eventRepository, userRepository);
    }

    private static Event eventWithId() {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private static User crewWithId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("crew1");
        user.setRole(UserRole.CREW);
        return user;
    }

    private static AuthenticatedPrincipal principalFor(User user) {
        return new AuthenticatedPrincipal(user.getId(), user.getUsername(), user.getRole(), false);
    }

    @Test
    void vendorPassDoesNotRequireStallNumber() {
        Event event = eventWithId();
        User crew = crewWithId();
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.of(event));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        var response = service.create(
                principalFor(crew), new CreateLabourPassRequest(LabourPassType.VENDOR, 3, "9876543210", null));

        assertThat(response.passType()).isEqualTo(LabourPassType.VENDOR);
    }

    @Test
    void exhibitorPassRequiresStallNumberAtServiceLayer() {
        // Stall-number validation short-circuits before the active-event lookup, so no stubs needed there.
        User crew = crewWithId();

        assertThatThrownBy(() -> service.create(
                        principalFor(crew), new CreateLabourPassRequest(LabourPassType.EXHIBITOR, 2, "9876543210", null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void fabricatorLabourPassRequiresStallNumberAtServiceLayer() {
        User crew = crewWithId();

        assertThatThrownBy(() -> service.create(
                        principalFor(crew),
                        new CreateLabourPassRequest(LabourPassType.FABRICATOR_LABOUR, 1, "9876543210", "  ")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void exhibitorPassWithStallNumberSucceeds() {
        Event event = eventWithId();
        User crew = crewWithId();
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.of(event));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        var response = service.create(
                principalFor(crew),
                new CreateLabourPassRequest(LabourPassType.EXHIBITOR, 2, "9876543210", "A-12"));

        assertThat(response.stallNumber()).isEqualTo("A-12");
    }

    @Test
    void createFailsWhenNoActiveEvent() {
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.empty());
        User crew = crewWithId();

        assertThatThrownBy(() -> service.create(
                        principalFor(crew), new CreateLabourPassRequest(LabourPassType.VENDOR, 1, "9876543210", null)))
                .isInstanceOf(ConflictException.class);
    }
}
