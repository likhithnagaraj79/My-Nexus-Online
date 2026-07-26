package com.exhibitorreg.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEventRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {

    @AssertTrue(message = "endDate must not be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
