package com.exhibitorreg.conferencedelegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.common.exception.CaptchaVerificationException;
import com.exhibitorreg.common.exception.GoneException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.conferencedelegate.dto.SubmitDelegateRegistrationRequest;
import com.exhibitorreg.publicregistration.RecaptchaVerificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PublicDelegateRegistrationServiceTest {

    @Mock
    private ConferenceDelegateRegistrationLinkRepository linkRepository;

    @Mock
    private ConferenceDelegateRepository delegateRepository;

    @Mock
    private RecaptchaVerificationService recaptchaVerificationService;

    private PublicDelegateRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new PublicDelegateRegistrationService(linkRepository, delegateRepository, recaptchaVerificationService);
    }

    private static ConferenceDelegateRegistrationLink linkWithId(boolean active, Instant expiresAt) {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        event.setName("Expo 2026");
        event.setStartDate(LocalDate.of(2026, 8, 1));
        event.setEndDate(LocalDate.of(2026, 8, 3));

        ConferenceDelegateRegistrationLink link = new ConferenceDelegateRegistrationLink();
        ReflectionTestUtils.setField(link, "id", UUID.randomUUID());
        link.setEvent(event);
        link.setActive(active);
        link.setExpiresAt(expiresAt);
        return link;
    }

    private static SubmitDelegateRegistrationRequest validRequest() {
        return new SubmitDelegateRegistrationRequest(
                "Alice", "Acme Exhibits", "Sales", "9876543210", "alice@example.com", "captcha-token");
    }

    @Test
    void getLinkInfoThrowsNotFoundForUnknownLink() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLinkInfo(linkId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLinkInfoThrowsNotFoundForInactiveLink() {
        ConferenceDelegateRegistrationLink link = linkWithId(false, null);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.getLinkInfo(link.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLinkInfoThrowsGoneForExpiredLink() {
        ConferenceDelegateRegistrationLink link = linkWithId(true, Instant.now().minus(1, ChronoUnit.DAYS));
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.getLinkInfo(link.getId())).isInstanceOf(GoneException.class);
    }

    @Test
    void getLinkInfoSucceedsForActiveUnexpiredLink() {
        ConferenceDelegateRegistrationLink link = linkWithId(true, Instant.now().plus(1, ChronoUnit.DAYS));
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        var response = service.getLinkInfo(link.getId());

        assertThat(response.eventName()).isEqualTo("Expo 2026");
    }

    @Test
    void submitFailsWhenCaptchaVerificationFails() {
        ConferenceDelegateRegistrationLink link = linkWithId(true, null);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        when(recaptchaVerificationService.verify(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.submit(link.getId(), validRequest(), "127.0.0.1"))
                .isInstanceOf(CaptchaVerificationException.class);

        verify(delegateRepository, never()).save(any());
    }

    @Test
    void submitCreatesDelegateOnSuccess() {
        ConferenceDelegateRegistrationLink link = linkWithId(true, null);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        when(recaptchaVerificationService.verify(anyString(), any())).thenReturn(true);

        service.submit(link.getId(), validRequest(), "127.0.0.1");

        var captor = org.mockito.ArgumentCaptor.forClass(ConferenceDelegate.class);
        verify(delegateRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
        assertThat(captor.getValue().getCompanyName()).isEqualTo("Acme Exhibits");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
    }
}
