package com.exhibitorreg.organiser;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.organiser.dto.CreateLinkRequest;
import com.exhibitorreg.organiser.dto.LinkResponse;
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
@RequestMapping("/api/organiser/links")
public class RegistrationLinkController {

    private final RegistrationLinkService registrationLinkService;

    public RegistrationLinkController(RegistrationLinkService registrationLinkService) {
        this.registrationLinkService = registrationLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody CreateLinkRequest request) {
        return registrationLinkService.createLink(principal, request);
    }

    @GetMapping
    public List<LinkResponse> list() {
        return registrationLinkService.listLinks();
    }

    @PatchMapping("/{id}/deactivate")
    public LinkResponse deactivate(@PathVariable UUID id) {
        return registrationLinkService.deactivate(id);
    }
}
