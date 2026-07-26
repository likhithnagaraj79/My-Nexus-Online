package com.exhibitorreg.crew.exhibitorpass.dto;

import java.util.List;
import java.util.UUID;

/** Exactly one of {@code personIds} or {@code companyId} must be provided. */
public record PrintRequest(List<UUID> personIds, UUID companyId) {
}
