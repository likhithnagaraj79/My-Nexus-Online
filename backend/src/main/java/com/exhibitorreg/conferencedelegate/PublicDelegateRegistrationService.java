package com.exhibitorreg.conferencedelegate;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.common.exception.CaptchaVerificationException;
import com.exhibitorreg.common.exception.GoneException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.conferencedelegate.dto.DelegateLinkInfoResponse;
import com.exhibitorreg.conferencedelegate.dto.DelegateSubmissionResponse;
import com.exhibitorreg.conferencedelegate.dto.SubmitDelegateRegistrationRequest;
import com.exhibitorreg.publicregistration.RecaptchaVerificationService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicDelegateRegistrationService {

    private final ConferenceDelegateRegistrationLinkRepository linkRepository;
    private final ConferenceDelegateRepository delegateRepository;
    private final RecaptchaVerificationService recaptchaVerificationService;

    public PublicDelegateRegistrationService(
            ConferenceDelegateRegistrationLinkRepository linkRepository,
            ConferenceDelegateRepository delegateRepository,
            RecaptchaVerificationService recaptchaVerificationService) {
        this.linkRepository = linkRepository;
        this.delegateRepository = delegateRepository;
        this.recaptchaVerificationService = recaptchaVerificationService;
    }

    @Transactional(readOnly = true)
    public DelegateLinkInfoResponse getLinkInfo(UUID linkId) {
        ConferenceDelegateRegistrationLink link = getValidLinkOrThrow(linkId);
        Event event = link.getEvent();
        return new DelegateLinkInfoResponse(link.getId(), event.getName(), event.getStartDate(), event.getEndDate());
    }

    @Transactional
    public DelegateSubmissionResponse submit(UUID linkId, SubmitDelegateRegistrationRequest request, String remoteIp) {
        ConferenceDelegateRegistrationLink link = getValidLinkOrThrow(linkId);

        if (!recaptchaVerificationService.verify(request.recaptchaToken(), remoteIp)) {
            throw new CaptchaVerificationException("Captcha verification failed.");
        }

        ConferenceDelegate delegate = new ConferenceDelegate();
        delegate.setLink(link);
        delegate.setName(request.name());
        delegate.setCompanyName(request.companyName());
        delegate.setDesignation(request.designation());
        delegate.setMobileNumber(request.mobileNumber());
        delegate.setEmail(request.email());
        delegateRepository.save(delegate);

        return new DelegateSubmissionResponse(delegate.getId());
    }

    private ConferenceDelegateRegistrationLink getValidLinkOrThrow(UUID linkId) {
        ConferenceDelegateRegistrationLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Registration link not found."));
        if (!link.isActive()) {
            throw new NotFoundException("Registration link not found.");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now())) {
            throw new GoneException("This registration link has expired.");
        }
        return link;
    }
}
