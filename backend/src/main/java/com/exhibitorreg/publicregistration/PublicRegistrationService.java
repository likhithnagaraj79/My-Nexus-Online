package com.exhibitorreg.publicregistration;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.common.exception.CaptchaVerificationException;
import com.exhibitorreg.common.exception.GoneException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.organiser.ExhibitorRegistrationLink;
import com.exhibitorreg.organiser.ExhibitorRegistrationLinkRepository;
import com.exhibitorreg.publicregistration.dto.CompanyOption;
import com.exhibitorreg.publicregistration.dto.LinkInfoResponse;
import com.exhibitorreg.publicregistration.dto.SubmissionResponse;
import com.exhibitorreg.publicregistration.dto.SubmitRegistrationRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicRegistrationService {

    private final ExhibitorRegistrationLinkRepository linkRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final ExhibitorSubmissionRepository submissionRepository;
    private final ExhibitorPersonRepository exhibitorPersonRepository;
    private final RecaptchaVerificationService recaptchaVerificationService;

    public PublicRegistrationService(
            ExhibitorRegistrationLinkRepository linkRepository,
            CompanyRepository companyRepository,
            CompanyService companyService,
            ExhibitorSubmissionRepository submissionRepository,
            ExhibitorPersonRepository exhibitorPersonRepository,
            RecaptchaVerificationService recaptchaVerificationService) {
        this.linkRepository = linkRepository;
        this.companyRepository = companyRepository;
        this.companyService = companyService;
        this.submissionRepository = submissionRepository;
        this.exhibitorPersonRepository = exhibitorPersonRepository;
        this.recaptchaVerificationService = recaptchaVerificationService;
    }

    @Transactional(readOnly = true)
    public LinkInfoResponse getLinkInfo(UUID linkId) {
        ExhibitorRegistrationLink link = getValidLinkOrThrow(linkId);
        Event event = link.getEvent();
        return new LinkInfoResponse(link.getId(), event.getName(), event.getStartDate(), event.getEndDate());
    }

    @Transactional(readOnly = true)
    public List<CompanyOption> searchCompanies(String query) {
        return companyRepository.findByNameContainingIgnoreCase(query).stream()
                .map(company -> new CompanyOption(company.getId(), company.getName()))
                .toList();
    }

    @Transactional
    public SubmissionResponse submit(UUID linkId, SubmitRegistrationRequest request, String remoteIp) {
        ExhibitorRegistrationLink link = getValidLinkOrThrow(linkId);

        if (!recaptchaVerificationService.verify(request.recaptchaToken(), remoteIp)) {
            throw new CaptchaVerificationException("Captcha verification failed.");
        }

        Company company = companyService.findOrCreate(request.companyName());

        ExhibitorSubmission submission = new ExhibitorSubmission();
        submission.setCompany(company);
        submission.setLink(link);
        submission.setDeclaredCount(request.people().size());
        submissionRepository.save(submission);

        List<ExhibitorPerson> people = request.people().stream()
                .map(personInput -> {
                    ExhibitorPerson person = new ExhibitorPerson();
                    person.setSubmission(submission);
                    person.setCompany(company);
                    person.setName(personInput.name());
                    person.setDesignation(personInput.designation());
                    return person;
                })
                .toList();
        exhibitorPersonRepository.saveAll(people);

        return new SubmissionResponse(submission.getId(), company.getId(), people.size());
    }

    private ExhibitorRegistrationLink getValidLinkOrThrow(UUID linkId) {
        ExhibitorRegistrationLink link = linkRepository.findById(linkId)
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
