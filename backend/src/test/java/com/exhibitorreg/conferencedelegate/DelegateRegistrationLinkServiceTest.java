package com.exhibitorreg.conferencedelegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.admin.EventRepository;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.conferencedelegate.dto.CreateDelegateLinkRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DelegateRegistrationLinkServiceTest {

    @Mock
    private ConferenceDelegateRegistrationLinkRepository linkRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    private DelegateRegistrationLinkService service;

    @BeforeEach
    void setUp() {
        service = new DelegateRegistrationLinkService(
                linkRepository, eventRepository, userRepository, "https://localhost:5173/register-delegate/");
    }

    private static Event eventWithId() {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private static User userWithId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("organiser1");
        user.setRole(UserRole.ORGANISER);
        return user;
    }

    @Test
    void createLinkFailsWhenNoEventIsActive() {
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.empty());
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), "organiser1", UserRole.ORGANISER, false);

        assertThatThrownBy(() -> service.createLink(principal, new CreateDelegateLinkRequest(null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createLinkBuildsFullPublicUrlFromLinkId() {
        Event event = eventWithId();
        User organiser = userWithId();
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.of(event));
        when(userRepository.findById(organiser.getId())).thenReturn(Optional.of(organiser));

        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(organiser.getId(), organiser.getUsername(), UserRole.ORGANISER, false);

        var response = service.createLink(principal, new CreateDelegateLinkRequest(null));

        assertThat(response.publicUrl()).startsWith("https://localhost:5173/register-delegate/");
        assertThat(response.active()).isTrue();
    }

    @Test
    void deactivateSetsFlagFalse() {
        ConferenceDelegateRegistrationLink link = new ConferenceDelegateRegistrationLink();
        ReflectionTestUtils.setField(link, "id", UUID.randomUUID());
        link.setActive(true);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        var response = service.deactivate(link.getId());

        assertThat(response.active()).isFalse();
    }
}
