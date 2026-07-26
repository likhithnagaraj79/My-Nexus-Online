package com.exhibitorreg.crew.labourpass;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.crew.labourpass.dto.CreateLabourPassRequest;
import com.exhibitorreg.crew.labourpass.dto.LabourPassSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crew/labour-passes")
public class LabourPassController {

    private final LabourPassService labourPassService;

    public LabourPassController(LabourPassService labourPassService) {
        this.labourPassService = labourPassService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabourPassSummary create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateLabourPassRequest request) {
        return labourPassService.create(principal, request);
    }

    @GetMapping
    public List<LabourPassSummary> list(@RequestParam UUID eventId) {
        return labourPassService.listByEvent(eventId);
    }
}
