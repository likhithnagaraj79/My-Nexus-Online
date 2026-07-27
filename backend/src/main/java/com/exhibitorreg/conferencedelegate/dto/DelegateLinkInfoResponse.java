package com.exhibitorreg.conferencedelegate.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DelegateLinkInfoResponse(UUID linkId, String eventName, LocalDate eventStartDate, LocalDate eventEndDate) {
}
