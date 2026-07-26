package com.exhibitorreg.validator;

import com.exhibitorreg.admin.dto.EventDayResponse;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.validator.dto.ScanRequest;
import com.exhibitorreg.validator.dto.ScanResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validator")
public class ValidatorController {

    private final ScanService scanService;

    public ValidatorController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping("/event-days")
    public List<EventDayResponse> listEventDays(@RequestParam UUID eventId) {
        return scanService.listEventDays(eventId);
    }

    @PostMapping("/scans")
    public ScanResponse scan(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @Valid @RequestBody ScanRequest request) {
        return scanService.scan(principal, request);
    }
}
