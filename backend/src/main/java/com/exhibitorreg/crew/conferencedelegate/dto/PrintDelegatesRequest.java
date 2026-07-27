package com.exhibitorreg.crew.conferencedelegate.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record PrintDelegatesRequest(@NotEmpty List<UUID> personIds) {
}
