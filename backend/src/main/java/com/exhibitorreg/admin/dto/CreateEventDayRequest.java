package com.exhibitorreg.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CreateEventDayRequest(@Positive int dayNumber, @NotNull LocalDate date) {
}
