package com.exhibitorreg.crew.conferencedelegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.auth.UserRole;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.conferencedelegate.ConferenceDelegate;
import com.exhibitorreg.conferencedelegate.ConferenceDelegateRepository;
import com.exhibitorreg.crew.conferencedelegate.dto.PrintDelegatesRequest;
import com.exhibitorreg.crew.conferencedelegate.dto.UpdateDelegateRequest;
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
class ConferenceDelegatePassServiceTest {

    @Mock
    private ConferenceDelegateRepository delegateRepository;

    @Mock
    private UserRepository userRepository;

    private ConferenceDelegatePassService service;

    @BeforeEach
    void setUp() {
        service = new ConferenceDelegatePassService(delegateRepository, userRepository);
    }

    private static ConferenceDelegate delegateWithId() {
        ConferenceDelegate delegate = new ConferenceDelegate();
        ReflectionTestUtils.setField(delegate, "id", UUID.randomUUID());
        delegate.setName("Alice");
        delegate.setCompanyName("Acme Exhibits");
        delegate.setDesignation("Sales");
        delegate.setMobileNumber("9876543210");
        delegate.setEmail("alice@example.com");
        return delegate;
    }

    private static User crewWithId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("crew1");
        user.setRole(UserRole.CREW);
        return user;
    }

    @Test
    void printMarksAllSelectedDelegatesAsPrinted() {
        ConferenceDelegate delegate1 = delegateWithId();
        ConferenceDelegate delegate2 = delegateWithId();
        User crew = crewWithId();
        when(delegateRepository.findAllById(List.of(delegate1.getId(), delegate2.getId())))
                .thenReturn(List.of(delegate1, delegate2));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);
        var results = service.print(principal, new PrintDelegatesRequest(List.of(delegate1.getId(), delegate2.getId())));

        assertThat(results).hasSize(2);
        assertThat(delegate1.isPrinted()).isTrue();
        assertThat(delegate1.getPrintedBy()).isEqualTo(crew);
        assertThat(delegate2.isPrinted()).isTrue();
    }

    @Test
    void printIsIdempotentForAlreadyPrintedDelegates() {
        ConferenceDelegate delegate = delegateWithId();
        delegate.setPrinted(true);
        User crew = crewWithId();
        when(delegateRepository.findAllById(List.of(delegate.getId()))).thenReturn(List.of(delegate));
        when(userRepository.findById(crew.getId())).thenReturn(Optional.of(crew));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(crew.getId(), crew.getUsername(), UserRole.CREW, false);
        var results = service.print(principal, new PrintDelegatesRequest(List.of(delegate.getId())));

        assertThat(results).hasSize(1);
        assertThat(delegate.isPrinted()).isTrue();
    }

    @Test
    void updateFillsInAllFieldsIncludingABlankName() {
        ConferenceDelegate delegate = delegateWithId();
        delegate.setName(null); // as CSV import would leave it
        when(delegateRepository.findById(delegate.getId())).thenReturn(Optional.of(delegate));

        var result = service.update(
                delegate.getId(),
                new UpdateDelegateRequest("Bob", "New Co", "Lead", "9998887776", "bob@example.com"));

        assertThat(result.name()).isEqualTo("Bob");
        assertThat(delegate.getName()).isEqualTo("Bob");
        assertThat(delegate.getCompanyName()).isEqualTo("New Co");
        assertThat(delegate.getDesignation()).isEqualTo("Lead");
        assertThat(delegate.getMobileNumber()).isEqualTo("9998887776");
        assertThat(delegate.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void updateThrowsNotFoundForUnknownDelegate() {
        UUID id = UUID.randomUUID();
        when(delegateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        id, new UpdateDelegateRequest("Bob", "New Co", "Lead", "9998887776", "bob@example.com")))
                .isInstanceOf(NotFoundException.class);
    }
}
