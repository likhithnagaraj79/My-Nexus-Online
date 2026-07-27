package com.exhibitorreg.crew.conferencedelegate;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.crew.conferencedelegate.dto.DelegatePassSummary;
import com.exhibitorreg.crew.conferencedelegate.dto.PrintDelegatesRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crew/conference-delegates")
public class ConferenceDelegatePassController {

    private final ConferenceDelegatePassService conferenceDelegatePassService;

    public ConferenceDelegatePassController(ConferenceDelegatePassService conferenceDelegatePassService) {
        this.conferenceDelegatePassService = conferenceDelegatePassService;
    }

    @GetMapping
    public List<DelegatePassSummary> list(
            @RequestParam(required = false) Boolean printed, @RequestParam(required = false) String q) {
        return conferenceDelegatePassService.list(printed, q);
    }

    @PostMapping("/print")
    public List<DelegatePassSummary> print(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @Valid @RequestBody PrintDelegatesRequest request) {
        return conferenceDelegatePassService.print(principal, request);
    }
}
