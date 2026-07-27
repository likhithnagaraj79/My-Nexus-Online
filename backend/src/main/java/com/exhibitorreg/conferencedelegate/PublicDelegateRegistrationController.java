package com.exhibitorreg.conferencedelegate;

import com.exhibitorreg.common.web.ClientIpResolver;
import com.exhibitorreg.conferencedelegate.dto.DelegateLinkInfoResponse;
import com.exhibitorreg.conferencedelegate.dto.DelegateSubmissionResponse;
import com.exhibitorreg.conferencedelegate.dto.SubmitDelegateRegistrationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/delegate-links")
public class PublicDelegateRegistrationController {

    private final PublicDelegateRegistrationService publicDelegateRegistrationService;

    public PublicDelegateRegistrationController(PublicDelegateRegistrationService publicDelegateRegistrationService) {
        this.publicDelegateRegistrationService = publicDelegateRegistrationService;
    }

    @GetMapping("/{linkId}")
    public DelegateLinkInfoResponse getLink(@PathVariable UUID linkId) {
        return publicDelegateRegistrationService.getLinkInfo(linkId);
    }

    @PostMapping("/{linkId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public DelegateSubmissionResponse submit(
            @PathVariable UUID linkId,
            @Valid @RequestBody SubmitDelegateRegistrationRequest request,
            HttpServletRequest servletRequest) {
        return publicDelegateRegistrationService.submit(linkId, request, ClientIpResolver.resolve(servletRequest));
    }
}
