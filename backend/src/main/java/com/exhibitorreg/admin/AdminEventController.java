package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.CreateEventDayRequest;
import com.exhibitorreg.admin.dto.CreateEventRequest;
import com.exhibitorreg.admin.dto.EventDayResponse;
import com.exhibitorreg.admin.dto.EventResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return adminEventService.createEvent(request);
    }

    @GetMapping
    public List<EventResponse> listEvents() {
        return adminEventService.listEvents();
    }

    @PostMapping("/{id}/activate")
    public EventResponse activate(@PathVariable UUID id) {
        return adminEventService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public EventResponse deactivate(@PathVariable UUID id) {
        return adminEventService.deactivate(id);
    }

    @PostMapping("/{id}/days")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDayResponse createEventDay(@PathVariable UUID id, @Valid @RequestBody CreateEventDayRequest request) {
        return adminEventService.createEventDay(id, request);
    }

    @GetMapping("/{id}/days")
    public List<EventDayResponse> listEventDays(@PathVariable UUID id) {
        return adminEventService.listEventDays(id);
    }
}
