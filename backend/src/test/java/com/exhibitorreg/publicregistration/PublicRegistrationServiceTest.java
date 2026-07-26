package com.exhibitorreg.publicregistration;

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
import com.exhibitorreg.organiser.ExhibitorRegistrationLink;
import com.exhibitorreg.organiser.ExhibitorRegistrationLinkRepository;
import com.exhibitorreg.publicregistration.dto.SubmitRegistrationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
class PublicRegistrationServiceTest {

    @Mock
    private ExhibitorRegistrationLinkRepository linkRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyService companyService;

    @Mock
    private ExhibitorSubmissionRepository submissionRepository;

    @Mock
    private ExhibitorPersonRepository exhibitorPersonRepository;

    @Mock
    private RecaptchaVerificationService recaptchaVerificationService;

    private PublicRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new PublicRegistrationService(
                linkRepository, companyRepository, companyService, submissionRepository,
                exhibitorPersonRepository, recaptchaVerificationService);
    }

    private static ExhibitorRegistrationLink linkWithId(boolean active, Instant expiresAt) {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        event.setName("Expo 2026");
        event.setStartDate(LocalDate.of(2026, 8, 1));
        event.setEndDate(LocalDate.of(2026, 8, 3));

        ExhibitorRegistrationLink link = new ExhibitorRegistrationLink();
        ReflectionTestUtils.setField(link, "id", UUID.randomUUID());
        link.setEvent(event);
        link.setActive(active);
        link.setExpiresAt(expiresAt);
        return link;
    }

    private static SubmitRegistrationRequest validRequest() {
        return new SubmitRegistrationRequest(
                "Acme Exhibits",
                List.of(new SubmitRegistrationRequest.PersonInput("Alice", "Sales")),
                "captcha-token");
    }

    @Test
    void getLinkInfoThrowsNotFoundForUnknownLink() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLinkInfo(linkId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLinkInfoThrowsNotFoundForInactiveLink() {
        ExhibitorRegistrationLink link = linkWithId(false, null);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.getLinkInfo(link.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLinkInfoThrowsGoneForExpiredLink() {
        ExhibitorRegistrationLink link = linkWithId(true, Instant.now().minus(1, ChronoUnit.DAYS));
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service.getLinkInfo(link.getId())).isInstanceOf(GoneException.class);
    }

    @Test
    void getLinkInfoSucceedsForActiveUnexpiredLink() {
        ExhibitorRegistrationLink link = linkWithId(true, Instant.now().plus(1, ChronoUnit.DAYS));
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

        var response = service.getLinkInfo(link.getId());

        assertThat(response.eventName()).isEqualTo("Expo 2026");
    }

    @Test
    void submitFailsWhenCaptchaVerificationFails() {
        ExhibitorRegistrationLink link = linkWithId(true, null);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        when(recaptchaVerificationService.verify(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.submit(link.getId(), validRequest(), "127.0.0.1"))
                .isInstanceOf(CaptchaVerificationException.class);

        verify(companyService, never()).findOrCreate(anyString());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitCreatesCompanySubmissionAndPeopleOnSuccess() {
        ExhibitorRegistrationLink link = linkWithId(true, null);
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        when(recaptchaVerificationService.verify(anyString(), any())).thenReturn(true);

        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        company.setName("Acme Exhibits");
        when(companyService.findOrCreate("Acme Exhibits")).thenReturn(company);

        var response = service.submit(link.getId(), validRequest(), "127.0.0.1");

        assertThat(response.companyId()).isEqualTo(company.getId());
        assertThat(response.personCount()).isEqualTo(1);
        verify(submissionRepository).save(any(ExhibitorSubmission.class));
        verify(exhibitorPersonRepository).saveAll(any());
    }
}
