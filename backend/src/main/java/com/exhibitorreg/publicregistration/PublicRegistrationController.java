package com.exhibitorreg.publicregistration;

import com.exhibitorreg.common.web.ClientIpResolver;
import com.exhibitorreg.publicregistration.dto.CompanyOption;
import com.exhibitorreg.publicregistration.dto.LinkInfoResponse;
import com.exhibitorreg.publicregistration.dto.SubmissionResponse;
import com.exhibitorreg.publicregistration.dto.SubmitRegistrationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicRegistrationController {

    private final PublicRegistrationService publicRegistrationService;

    public PublicRegistrationController(PublicRegistrationService publicRegistrationService) {
        this.publicRegistrationService = publicRegistrationService;
    }

    @GetMapping("/links/{linkId}")
    public LinkInfoResponse getLink(@PathVariable UUID linkId) {
        return publicRegistrationService.getLinkInfo(linkId);
    }

    @GetMapping("/companies")
    public List<CompanyOption> searchCompanies(@RequestParam String query) {
        return publicRegistrationService.searchCompanies(query);
    }

    @PostMapping("/links/{linkId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(
            @PathVariable UUID linkId,
            @Valid @RequestBody SubmitRegistrationRequest request,
            HttpServletRequest servletRequest) {
        return publicRegistrationService.submit(linkId, request, ClientIpResolver.resolve(servletRequest));
    }
}
