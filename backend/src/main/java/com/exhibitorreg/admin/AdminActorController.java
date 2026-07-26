package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.ActorSummary;
import com.exhibitorreg.admin.dto.CreateCrewOrValidatorRequest;
import com.exhibitorreg.admin.dto.CreateOrganiserRequest;
import com.exhibitorreg.admin.dto.CreatedActorResponse;
import com.exhibitorreg.admin.dto.TotpQrResponse;
import com.exhibitorreg.auth.UserRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/actors")
public class AdminActorController {

    private final AdminActorService adminActorService;

    public AdminActorController(AdminActorService adminActorService) {
        this.adminActorService = adminActorService;
    }

    @PostMapping("/organisers")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedActorResponse createOrganiser(@Valid @RequestBody CreateOrganiserRequest request) {
        return adminActorService.createOrganiser(request);
    }

    @PostMapping("/crew")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedActorResponse createCrew(@Valid @RequestBody CreateCrewOrValidatorRequest request) {
        return adminActorService.createCrew(request);
    }

    @PostMapping("/validators")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedActorResponse createValidator(@Valid @RequestBody CreateCrewOrValidatorRequest request) {
        return adminActorService.createValidator(request);
    }

    @GetMapping
    public PagedModel<ActorSummary> listActors(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return new PagedModel<>(adminActorService.listActors(role, active, pageable));
    }

    @PostMapping("/{id}/unlock")
    public void unlock(@PathVariable UUID id) {
        adminActorService.unlock(id);
    }

    @PatchMapping("/{id}/activate")
    public void activate(@PathVariable UUID id) {
        adminActorService.setActive(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivate(@PathVariable UUID id) {
        adminActorService.setActive(id, false);
    }

    @PostMapping("/{id}/totp-qr/regenerate")
    public TotpQrResponse regenerateTotpQr(@PathVariable UUID id) {
        return adminActorService.regenerateTotpQr(id);
    }
}
