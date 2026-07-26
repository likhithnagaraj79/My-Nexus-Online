package com.exhibitorreg.admin.dto;

import com.exhibitorreg.admin.EventDay;
import java.util.UUID;

public record EventDayResponse(UUID id, int dayNumber) {

    public static EventDayResponse from(EventDay eventDay) {
        return new EventDayResponse(eventDay.getId(), eventDay.getDayNumber());
    }
}
