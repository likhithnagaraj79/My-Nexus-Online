package com.exhibitorreg.conferencedelegate;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.conferencedelegate.dto.CreateDelegateLinkRequest;
import com.exhibitorreg.conferencedelegate.dto.DelegateLinkResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organiser/delegate-links")
public class DelegateRegistrationLinkController {

    private final DelegateRegistrationLinkService delegateRegistrationLinkService;

    public DelegateRegistrationLinkController(DelegateRegistrationLinkService delegateRegistrationLinkService) {
        this.delegateRegistrationLinkService = delegateRegistrationLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DelegateLinkResponse create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody CreateDelegateLinkRequest request) {
        return delegateRegistrationLinkService.createLink(principal, request);
    }

    @GetMapping
    public List<DelegateLinkResponse> list() {
        return delegateRegistrationLinkService.listLinks();
    }

    @PatchMapping("/{id}/deactivate")
    public DelegateLinkResponse deactivate(@PathVariable UUID id) {
        return delegateRegistrationLinkService.deactivate(id);
    }
}
