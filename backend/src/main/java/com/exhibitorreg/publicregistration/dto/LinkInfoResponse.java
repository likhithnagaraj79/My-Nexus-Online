package com.exhibitorreg.publicregistration.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LinkInfoResponse(UUID linkId, String eventName, LocalDate eventStartDate, LocalDate eventEndDate) {
}
