package com.exhibitorreg.admin.dto;

import com.exhibitorreg.admin.Event;
import java.time.LocalDate;
import java.util.UUID;

public record EventResponse(UUID id, String name, LocalDate startDate, LocalDate endDate, boolean active) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(), event.getName(), event.getStartDate(), event.getEndDate(), event.isActive());
    }
}
