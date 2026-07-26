package com.exhibitorreg.common;

import com.exhibitorreg.admin.AdminEventService;
import com.exhibitorreg.admin.dto.EventDayResponse;
import com.exhibitorreg.admin.dto.EventResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only event/event-day discovery for non-Admin roles. Crew, Organiser, and Validator
 * all need an eventId/eventDayId for their own endpoints but have no other way to look one
 * up — /api/admin/events is Admin-only. Reuses AdminEventService rather than duplicating
 * query logic.
 */
@RestController
@RequestMapping("/api/common/events")
public class CommonEventController {

    private final AdminEventService adminEventService;

    public CommonEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping("/active")
    public EventResponse active() {
        return adminEventService.getActiveEvent();
    }

    @GetMapping("/{id}/days")
    public List<EventDayResponse> days(@PathVariable UUID id) {
        return adminEventService.listEventDays(id);
    }
}
