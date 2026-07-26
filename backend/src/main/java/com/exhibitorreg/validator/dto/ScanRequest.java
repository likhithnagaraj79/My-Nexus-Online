package com.exhibitorreg.validator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ScanRequest(@NotNull UUID eventDayId, @NotBlank String qrPayload) {
}
