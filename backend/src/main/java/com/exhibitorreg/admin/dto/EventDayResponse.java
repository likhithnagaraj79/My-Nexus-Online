package com.exhibitorreg.admin.dto;

import com.exhibitorreg.admin.EventDay;
import java.time.LocalDate;
import java.util.UUID;

public record EventDayResponse(UUID id, int dayNumber, LocalDate date) {

    public static EventDayResponse from(EventDay eventDay) {
        return new EventDayResponse(eventDay.getId(), eventDay.getDayNumber(), eventDay.getDate());
    }
}
