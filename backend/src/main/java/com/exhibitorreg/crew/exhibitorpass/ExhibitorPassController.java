package com.exhibitorreg.crew.exhibitorpass;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.crew.exhibitorpass.dto.ExhibitorPassSummary;
import com.exhibitorreg.crew.exhibitorpass.dto.IssueRequest;
import com.exhibitorreg.crew.exhibitorpass.dto.PrintRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crew/exhibitor-passes")
public class ExhibitorPassController {

    private final ExhibitorPassService exhibitorPassService;

    public ExhibitorPassController(ExhibitorPassService exhibitorPassService) {
        this.exhibitorPassService = exhibitorPassService;
    }

    /** Visible to Crew (owning role), Organiser, and Admin ("all exhibitor submitted details
     * visible to Organiser") — the URL rule for this path is only `authenticated()` in
     * SecurityConfig; the actual role restriction is enforced here. */
    @GetMapping
    @PreAuthorize("hasAnyRole('CREW','ORGANISER','ADMIN')")
    public List<ExhibitorPassSummary> list(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) Boolean printed,
            @RequestParam(required = false) Boolean issued,
            @RequestParam(required = false) String q) {
        return exhibitorPassService.list(companyId, printed, issued, q);
    }

    @PostMapping("/print")
    public List<ExhibitorPassSummary> print(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody PrintRequest request) {
        return exhibitorPassService.print(principal, request);
    }

    @PatchMapping("/{id}/issue")
    public ExhibitorPassSummary issue(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody IssueRequest request) {
        return exhibitorPassService.issue(principal, id, request);
    }

    @GetMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qrCode(@PathVariable UUID id) {
        return exhibitorPassService.generateQrPng(id);
    }
}
